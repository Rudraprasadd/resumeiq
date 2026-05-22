package com.resumeiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeiq.exception.ResourceNotFoundException;
import com.resumeiq.model.Analysis;
import com.resumeiq.model.Resume;
import com.resumeiq.model.User;
import com.resumeiq.repository.AnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository analysisRepository;
    private final GeminiAiService geminiAiService;   // swapped from ClaudeAiService
    private final ResumeService resumeService;
    private final UsageService usageService;
    private final ObjectMapper objectMapper;

    public AnalysisService(AnalysisRepository analysisRepository,
                           GeminiAiService geminiAiService,
                           ResumeService resumeService,
                           UsageService usageService,
                           ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.geminiAiService    = geminiAiService;
        this.resumeService      = resumeService;
        this.usageService       = usageService;
        this.objectMapper       = objectMapper;
    }

    /**
     * Full analysis flow:
     * 1. Enforce quota (FREE users: max 3/month)
     * 2. Check DB cache (same resume + JD = no AI call needed)
     * 3. Call Gemini AI
     * 4. Parse and save result
     * 5. Return analysis
     */
    @Transactional
    public Analysis runAnalysis(UUID resumeId, String jobDescription,
                                String jobTitle, String companyName, User user) {

        // 1. Quota check
        usageService.checkAndEnforceQuota(user);

        // 2. Load resume (also verifies it belongs to this user)
        Resume resume = resumeService.getResume(resumeId, user);

        if (resume.getContentText() == null || resume.getContentText().isBlank()) {
            throw new IllegalStateException("Resume has no extractable text. Please re-upload.");
        }

        // 3. Cache check — same resume + same JD = return cached result, skip AI call
        String cacheKey = buildCacheKey(resume.getContentText(), jobDescription);
        var cached = analysisRepository.findByAiCacheKey(cacheKey);
        if (cached.isPresent()) {
            log.info("Cache hit for analysis (key: {}...)", cacheKey.substring(0, 8));
            return cached.get();
        }

        // 4. Call Gemini AI
        long start = System.currentTimeMillis();
        String aiJsonResponse = geminiAiService.analyseResume(
                resume.getContentText(), jobDescription, jobTitle, companyName);
        long elapsed = System.currentTimeMillis() - start;

        // 5. Parse and save
        Analysis analysis = parseAndBuildAnalysis(
                aiJsonResponse, resume, user,
                jobDescription, jobTitle, companyName,
                cacheKey, elapsed);

        Analysis saved = analysisRepository.save(analysis);
        log.info("Analysis {} saved for user {} ({}ms)", saved.getId(), user.getEmail(), elapsed);
        return saved;
    }

    public List<Analysis> getUserAnalyses(User user) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Analysis getAnalysis(UUID analysisId, User user) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis", analysisId.toString()));

        if (!analysis.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Analysis", analysisId.toString());
        }
        return analysis;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Analysis parseAndBuildAnalysis(String aiJson, Resume resume, User user,
                                           String jobDescription, String jobTitle,
                                           String companyName, String cacheKey, long elapsed) {
        try {
            JsonNode root = objectMapper.readTree(aiJson);

            Analysis a = new Analysis();
            a.setUser(user);
            a.setResume(resume);
            a.setJobDescription(jobDescription);
            a.setJobTitle(jobTitle);
            a.setCompanyName(companyName);
            a.setMatchScore(root.path("matchScore").asInt(0));
            a.setAtsScore(root.path("atsScore").asInt(0));
            a.setMissingKeywords(objectMapper.writeValueAsString(root.path("missingKeywords")));
            a.setKeywordMatches(objectMapper.writeValueAsString(root.path("keywordMatches")));
            a.setRewriteSuggestions(objectMapper.writeValueAsString(root.path("rewriteSuggestions")));
            a.setCoverLetter(root.path("coverLetter").asText(""));
            a.setInterviewQuestions(objectMapper.writeValueAsString(root.path("interviewQuestions")));
            a.setAiCacheKey(cacheKey);
            a.setProcessingTimeMs(elapsed);
            return a;

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new RuntimeException("Failed to process AI response. Please try again.");
        }
    }

    private String buildCacheKey(String resumeText, String jobDescription) {
        try {
            String combined = resumeText + "|||" + jobDescription;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}