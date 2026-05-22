package com.resumeiq.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeiq.model.Analysis;
import com.resumeiq.model.Resume;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResumeAnalysisResponse {

    // ---- Resume upload response ----
    public record ResumeResponse(
        UUID id,
        String fileName,
        Integer fileSizeKb,
        LocalDateTime uploadedAt,
        boolean hasText
    ) {
        public static ResumeResponse from(Resume r) {
            return new ResumeResponse(
                r.getId(),
                r.getFileName(),
                r.getFileSizeKb(),
                r.getUploadedAt(),
                r.getContentText() != null && !r.getContentText().isBlank()
            );
        }
    }

    // ---- Full analysis response (what the AI returned) ----
    public record AnalysisResponse(
        UUID id,
        UUID resumeId,
        String jobTitle,
        String companyName,
        Integer matchScore,
        Integer atsScore,
        List<String> missingKeywords,
        Map<String, Boolean> keywordMatches,
        List<RewriteSuggestion> rewriteSuggestions,
        String coverLetter,
        List<String> interviewQuestions,
        Long processingTimeMs,
        LocalDateTime createdAt
    ) {
        public static AnalysisResponse from(Analysis a) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<String> missing = mapper.readValue(
                    a.getMissingKeywords(), new TypeReference<>() {});
                Map<String, Boolean> matches = mapper.readValue(
                    a.getKeywordMatches(), new TypeReference<>() {});
                List<RewriteSuggestion> rewrites = mapper.readValue(
                    a.getRewriteSuggestions(), new TypeReference<>() {});
                List<String> questions = mapper.readValue(
                    a.getInterviewQuestions(), new TypeReference<>() {});

                return new AnalysisResponse(
                    a.getId(),
                    a.getResume().getId(),
                    a.getJobTitle(),
                    a.getCompanyName(),
                    a.getMatchScore(),
                    a.getAtsScore(),
                    missing,
                    matches,
                    rewrites,
                    a.getCoverLetter(),
                    questions,
                    a.getProcessingTimeMs(),
                    a.getCreatedAt()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse analysis data", e);
            }
        }
    }

    // ---- Rewrite suggestion sub-object ----
    public record RewriteSuggestion(
        String original,
        String improved
    ) {}

    // ---- Summary for history list ----
    public record AnalysisSummary(
        UUID id,
        String jobTitle,
        String companyName,
        Integer matchScore,
        Integer atsScore,
        LocalDateTime createdAt
    ) {
        public static AnalysisSummary from(Analysis a) {
            return new AnalysisSummary(
                a.getId(),
                a.getJobTitle(),
                a.getCompanyName(),
                a.getMatchScore(),
                a.getAtsScore(),
                a.getCreatedAt()
            );
        }
    }

    // ---- Quota status (shown in dashboard) ----
    public record QuotaResponse(
        String plan,
        long usedThisMonth,
        long remaining,
        int monthlyLimit
    ) {}
}