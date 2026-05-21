package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "job_description", columnDefinition = "TEXT", nullable = false)
    private String jobDescription;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    // ---- AI Results ----

    @Column(name = "match_score")
    private Integer matchScore;

    /** JSON array: ["Java", "Spring Boot", "Kafka"] */
    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private String missingKeywords;

    /** JSON object: {"Java": true, "Python": false} */
    @Column(name = "keyword_matches", columnDefinition = "jsonb")
    private String keywordMatches;

    /** JSON array of {original, improved} objects */
    @Column(name = "rewrite_suggestions", columnDefinition = "jsonb")
    private String rewriteSuggestions;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    /** JSON array of interview question strings */
    @Column(name = "interview_questions", columnDefinition = "jsonb")
    private String interviewQuestions;

    /** SHA-256(resumeText + jd) — used as Redis cache key */
    @Column(name = "ai_cache_key")
    private String aiCacheKey;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}