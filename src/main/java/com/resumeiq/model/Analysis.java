package com.resumeiq.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyses")
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

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private String missingKeywords;

    @Column(name = "keyword_matches", columnDefinition = "jsonb")
    private String keywordMatches;

    @Column(name = "rewrite_suggestions", columnDefinition = "jsonb")
    private String rewriteSuggestions;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "interview_questions", columnDefinition = "jsonb")
    private String interviewQuestions;

    @Column(name = "ai_cache_key")
    private String aiCacheKey;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Analysis() {}

    public UUID getId()                    { return id; }
    public User getUser()                  { return user; }
    public Resume getResume()              { return resume; }
    public String getJobDescription()      { return jobDescription; }
    public String getJobTitle()            { return jobTitle; }
    public String getCompanyName()         { return companyName; }
    public Integer getMatchScore()         { return matchScore; }
    public String getMissingKeywords()     { return missingKeywords; }
    public String getKeywordMatches()      { return keywordMatches; }
    public String getRewriteSuggestions()  { return rewriteSuggestions; }
    public Integer getAtsScore()           { return atsScore; }
    public String getCoverLetter()         { return coverLetter; }
    public String getInterviewQuestions()  { return interviewQuestions; }
    public String getAiCacheKey()          { return aiCacheKey; }
    public Long getProcessingTimeMs()      { return processingTimeMs; }
    public LocalDateTime getCreatedAt()    { return createdAt; }

    public void setUser(User user)                  { this.user = user; }
    public void setResume(Resume resume)            { this.resume = resume; }
    public void setJobDescription(String v)         { this.jobDescription = v; }
    public void setJobTitle(String v)               { this.jobTitle = v; }
    public void setCompanyName(String v)            { this.companyName = v; }
    public void setMatchScore(Integer v)            { this.matchScore = v; }
    public void setMissingKeywords(String v)        { this.missingKeywords = v; }
    public void setKeywordMatches(String v)         { this.keywordMatches = v; }
    public void setRewriteSuggestions(String v)     { this.rewriteSuggestions = v; }
    public void setAtsScore(Integer v)              { this.atsScore = v; }
    public void setCoverLetter(String v)            { this.coverLetter = v; }
    public void setInterviewQuestions(String v)     { this.interviewQuestions = v; }
    public void setAiCacheKey(String v)             { this.aiCacheKey = v; }
    public void setProcessingTimeMs(Long v)         { this.processingTimeMs = v; }
}