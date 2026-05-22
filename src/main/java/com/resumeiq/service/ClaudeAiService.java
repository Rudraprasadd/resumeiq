package com.resumeiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the Claude API (Anthropic) to analyse a resume against a job description.
 *
 * We send ONE prompt that asks Claude to return a strict JSON object.
 * This avoids multiple API calls and keeps costs low.
 *
 * Expected response shape:
 * {
 *   "matchScore": 72,
 *   "atsScore": 68,
 *   "missingKeywords": ["Kafka", "Docker", "CI/CD"],
 *   "keywordMatches": {"Java": true, "Spring Boot": true, "Python": false},
 *   "rewriteSuggestions": [
 *     {"original": "Worked on backend APIs", "improved": "Designed and implemented 15+ REST APIs using Spring Boot..."}
 *   ],
 *   "coverLetter": "Dear Hiring Manager...",
 *   "interviewQuestions": ["Tell me about your Spring Boot experience...", ...]
 * }
 */
@Service
public class ClaudeAiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final long timeoutSeconds;

    public ClaudeAiService(
            @Value("${claude.api.key}") String apiKey,
            @Value("${claude.api.base-url}") String baseUrl,
            @Value("${claude.api.model}") String model,
            @Value("${claude.api.max-tokens}") int maxTokens,
            @Value("${claude.api.timeout-seconds}") long timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this.model          = model;
        this.maxTokens      = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper   = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    /**
     * Main method — send resume text + JD to Claude, get back structured analysis JSON string.
     * The caller (AnalysisService) is responsible for parsing and storing the result.
     */
    public String analyseResume(String resumeText, String jobDescription,
                                String jobTitle, String companyName) {
        String prompt = buildPrompt(resumeText, jobDescription, jobTitle, companyName);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        try {
            log.info("Calling Claude API for analysis (model: {})", model);
            long start = System.currentTimeMillis();

            String responseBody = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            log.info("Claude API responded in {}ms", elapsed);

            return extractJsonFromResponse(responseBody);

        } catch (WebClientResponseException e) {
            log.error("Claude API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service temporarily unavailable. Please try again.");
        }
    }

    // ---- Private helpers ----

    private String buildPrompt(String resumeText, String jd, String jobTitle, String companyName) {
        return """
            You are an expert ATS (Applicant Tracking System) and career coach.

            Analyse the resume below against the job description and return a JSON object ONLY.
            No explanation, no markdown, no code fences — pure JSON only.

            JOB TITLE: %s
            COMPANY: %s

            === JOB DESCRIPTION ===
            %s

            === RESUME ===
            %s

            Return this exact JSON structure (all fields required):
            {
              "matchScore": <integer 0-100, how well the resume matches the JD>,
              "atsScore": <integer 0-100, how well an ATS would parse this resume>,
              "missingKeywords": [<list of important keywords from JD missing from resume>],
              "keywordMatches": {<keyword: true/false for top 10 JD keywords>},
              "rewriteSuggestions": [
                {
                  "original": "<exact bullet point from resume that should be improved>",
                  "improved": "<rewritten version tailored to this JD, stronger action verbs, metrics>"
                }
              ],
              "coverLetter": "<complete tailored cover letter for this role, 3 paragraphs>",
              "interviewQuestions": [<5 likely interview questions based on the JD>]
            }

            Rules:
            - matchScore and atsScore must be integers between 0 and 100
            - missingKeywords: list only keywords actually in the JD but missing from resume
            - rewriteSuggestions: provide exactly 3 of the weakest bullet points with improvements
            - interviewQuestions: exactly 5 questions
            - Return ONLY the JSON object, nothing else
            """.formatted(
                jobTitle != null ? jobTitle : "Not specified",
                companyName != null ? companyName : "Not specified",
                jd,
                resumeText
            );
    }

    /**
     * Claude's response is a Messages API object — we extract the text content block
     * which should be our raw JSON string.
     */
    private String extractJsonFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("content");

        if (!content.isArray() || content.isEmpty()) {
            throw new RuntimeException("Unexpected response shape from Claude API");
        }

        // First content block, type "text"
        String text = content.get(0).path("text").asText();

        if (text.isBlank()) {
            throw new RuntimeException("Empty response from Claude API");
        }

        // Strip markdown code fences if Claude added them despite instructions
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }

        // Validate it's actually parseable JSON before returning
        objectMapper.readTree(text); // throws if not valid JSON
        return text;
    }
}