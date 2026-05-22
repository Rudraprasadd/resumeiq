package com.resumeiq.service;

import com.resumeiq.exception.ResourceNotFoundException;
import com.resumeiq.model.Resume;
import com.resumeiq.model.User;
import com.resumeiq.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final PdfExtractorService pdfExtractor;
    private final StorageService storageService;

    public ResumeService(ResumeRepository resumeRepository,
                         PdfExtractorService pdfExtractor,
                         StorageService storageService) {
        this.resumeRepository = resumeRepository;
        this.pdfExtractor     = pdfExtractor;
        this.storageService   = storageService;
    }

    /**
     * Upload a PDF resume:
     * 1. Extract text from PDF
     * 2. Save file to disk
     * 3. Save Resume record to DB
     */
    @Transactional
    public Resume uploadResume(MultipartFile file, User user) throws IOException {
        log.info("Processing resume upload for user: {}", user.getEmail());

        // 1. Extract text first — fail fast before saving anything
        String extractedText = pdfExtractor.extractText(file);

        // 2. Save file to disk
        String storageKey = storageService.store(file, user.getId());

        // 3. Save record to DB
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(file.getOriginalFilename());
        resume.setStorageKey(storageKey);
        resume.setContentText(extractedText);
        resume.setFileSizeKb((int) (file.getSize() / 1024));

        Resume saved = resumeRepository.save(resume);
        log.info("Resume saved with id: {}", saved.getId());
        return saved;
    }

    /**
     * Get all resumes for a user, newest first.
     */
    public List<Resume> getUserResumes(User user) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
    }

    /**
     * Get a single resume — verifies it belongs to the requesting user.
     */
    public Resume getResume(UUID resumeId, User user) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId.toString()));

        // Security: ensure user can only access their own resumes
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume", resumeId.toString());
        }

        return resume;
    }

    /**
     * Delete a resume and its file from disk.
     */
    @Transactional
    public void deleteResume(UUID resumeId, User user) {
        Resume resume = getResume(resumeId, user);
        storageService.delete(resume.getStorageKey());
        resumeRepository.delete(resume);
        log.info("Deleted resume {} for user {}", resumeId, user.getEmail());
    }
}