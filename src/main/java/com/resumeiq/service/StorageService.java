package com.resumeiq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves uploaded files to local disk under: {upload-dir}/{userId}/{uuid}.pdf
 *
 * NOTE: This is a local storage implementation for dev + small-scale prod.
 * When you need to scale to multiple servers, swap this class for an S3
 * implementation — the interface stays the same, only this class changes.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path rootLocation;

    public StorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootLocation); // create on startup if not exists
        log.info("File storage location: {}", rootLocation);
    }

    /**
     * Save a file and return the storage key (relative path from upload root).
     * e.g. "3a038212/f7c91d23-4a1b.pdf"
     */
    public String store(MultipartFile file, UUID userId) throws IOException {
        String uniqueFileName = UUID.randomUUID() + ".pdf";
        String storageKey = userId.toString() + "/" + uniqueFileName;

        Path userDir = rootLocation.resolve(userId.toString());
        Files.createDirectories(userDir);

        Path destination = userDir.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        log.debug("Stored file: {}", storageKey);
        return storageKey;
    }

    /**
     * Read a stored file back as bytes (for re-processing).
     */
    public byte[] load(String storageKey) throws IOException {
        Path file = rootLocation.resolve(storageKey).normalize();

        // Security: ensure resolved path is still under our upload root
        if (!file.startsWith(rootLocation)) {
            throw new SecurityException("Attempted path traversal: " + storageKey);
        }

        return Files.readAllBytes(file);
    }

    /**
     * Delete a stored file (when user deletes their resume).
     */
    public void delete(String storageKey) {
        try {
            Path file = rootLocation.resolve(storageKey).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", storageKey, e.getMessage());
        }
    }
}