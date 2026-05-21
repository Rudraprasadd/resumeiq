package com.resumeiq.repository;

import com.resumeiq.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdOrderByUploadedAtDesc(UUID userId);

    long countByUserId(UUID userId);
}