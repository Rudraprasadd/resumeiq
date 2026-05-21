package com.resumeiq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * For now: relative path under /uploads/{userId}/
     * Later: S3 object key like "resumes/{userId}/{uuid}.pdf"
     */
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    /**
     * Plain text extracted from the PDF.
     * Stored so we don't re-parse on every analysis.
     */
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "uploaded_at", updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}