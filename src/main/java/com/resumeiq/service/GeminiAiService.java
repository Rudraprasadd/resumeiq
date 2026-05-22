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
 * Calls the Google Gemini API (FREE tier — no card needed).
 *
 * Free limits: 1,500 requests/day, 15 RPM — more than enough to launch.
 *
 * Gemini API endpoint:
 *   POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 *
 * Request body shape:
 * {
 *   "contents": [{ "parts": [{ "text": "..." }] }],
 *   "generationConfig": { "maxOutputTokens": 1500, "temperature": 0.2 }
 * }
 *
 * Response shape:
 * {
 *   "candidates": [{
 *     "content": { "parts": [{ "text": "...JSON here..." }] }
 *   }]
 * }
 */
@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final long timeoutSeconds;
    private final String apiKey;

    public GeminiAiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.base-url}") String baseUrl,
            @Value("${gemini.api.model}") String model,
            @Value("${gemini.api.max-tokens}") int maxTokens,
            @Value("${gemini.api.timeout-seconds}") long timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this.apiKey         = apiKey;
        this.model          = model;
        this.maxTokens      = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper   = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Main method — send resume + JD to Gemini, get back structured JSON string.
     * Identical contract to the old ClaudeAiService.analyseResume() — AnalysisService
     * doesn't need to change its logic at all.
     */
    public String analyseResume(String resumeText, String jobDescription,
                                String jobTitle, String companyName) {

        String prompt = buildPrompt(resumeText, jobDescription, jobTitle, companyName);

        // Gemini request body
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "maxOutputTokens", maxTokens,
                "temperature",     0.2        // low temperature = consistent, structured output
            )
        );

        try {
            log.info("Calling Gemini API for analysis (model: {})", model);
            long start = System.currentTimeMillis();

            // API key is passed as query param for Gemini
            String responseBody = webClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            log.info("Gemini API responded in {}ms", elapsed);

            return extractJsonFromResponse(responseBody);

        } catch (WebClientResponseException e) {
            log.error("Gemini API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            // 429 = rate limit hit (15 RPM on free tier)
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException(
                    "AI service is busy right now. Please wait 10 seconds and try again.");
            }
            throw new RuntimeException("AI service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service temporarily unavailable. Please try again.");
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private String buildPrompt(String resumeText, String jd,
                                String jobTitle, String companyName) {
        return """
            You are an expert ATS (Applicant Tracking System) and career coach.

            Analyse the resume below against the job description and return a JSON object ONLY.
            No explanation, no markdown, no code fences — pure raw JSON only.

            JOB TITLE: %s
            COMPANY: %s

            === JOB DESCRIPTION ===
            %s

            === RESUME ===
            %s

            Return this exact JSON structure (all fields required):
            {
              "matchScore": <integer 0-100, how well resume matches the JD>,
              "atsScore": <integer 0-100, how well an ATS would parse this resume>,
              "missingKeywords": [<important keywords from JD missing in resume>],
              "keywordMatches": {<top 10 JD keywords mapped to true/false>},
              "rewriteSuggestions": [
                {
                  "original": "<exact weak bullet from resume>",
                  "improved": "<rewritten version with strong action verbs and metrics>"
                }
              ],
              "coverLetter": "<complete tailored cover letter, 3 paragraphs>",
              "interviewQuestions": [<exactly 5 likely interview questions based on JD>]
            }

            Rules:
            - matchScore and atsScore must be integers 0-100
            - missingKeywords: only keywords actually in JD but absent from resume
            - rewriteSuggestions: exactly 3 entries
            - interviewQuestions: exactly 5 entries
            - Return ONLY the JSON object — no markdown, no explanation, nothing else
            """.formatted(
                jobTitle     != null ? jobTitle     : "Not specified",
                companyName  != null ? companyName  : "Not specified",
                jd,
                resumeText
            );
    }

    /**
     * Gemini wraps response in: candidates[0].content.parts[0].text
     * Extract that text, strip any accidental markdown fences, validate JSON.
     */
    private String extractJsonFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Navigate: candidates → [0] → content → parts → [0] → text
        JsonNode text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text");

        if (text == null || text.isMissingNode()) {
            // Check if Gemini returned a safety block
            JsonNode finishReason = root.path("candidates").get(0).path("finishReason");
            throw new RuntimeException(
                "Empty response from Gemini API. Finish reason: " + finishReason.asText("unknown"));
        }

        String raw = text.asText().trim();

        // Strip markdown code fences if Gemini added them despite instructions
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("(?s)^```[a-z]*\\n?", "")
                     .replaceAll("```\\s*$", "")
                     .trim();
        }

        // Validate parseable JSON before returning
        objectMapper.readTree(raw); // throws JsonProcessingException if invalid
        return raw;
    }
}