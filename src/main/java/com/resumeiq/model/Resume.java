package com.resumeiq.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Resume() {}

    public UUID getId()              { return id; }
    public User getUser()            { return user; }
    public String getFileName()      { return fileName; }
    public String getStorageKey()    { return storageKey; }
    public String getContentText()   { return contentText; }
    public Integer getFileSizeKb()   { return fileSizeKb; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setUser(User user)               { this.user = user; }
    public void setFileName(String v)            { this.fileName = v; }
    public void setStorageKey(String v)          { this.storageKey = v; }
    public void setContentText(String v)         { this.contentText = v; }
    public void setFileSizeKb(Integer v)         { this.fileSizeKb = v; }
    public void setUploadedAt(LocalDateTime t)   { this.uploadedAt = t; }
}
