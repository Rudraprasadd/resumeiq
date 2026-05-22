package com.resumeiq.controller;

import com.resumeiq.dto.response.ResumeAnalysisResponse.ResumeResponse;
import com.resumeiq.model.Resume;
import com.resumeiq.model.User;
import com.resumeiq.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@Tag(name = "Resumes", description = "Upload and manage resume PDFs")
@SecurityRequirement(name = "bearerAuth")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    // POST /api/resumes/upload
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a PDF resume",
               description = "Accepts PDF up to 5MB, extracts text, stores in DB. Returns resume ID for use in analysis.")
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        Resume resume = resumeService.uploadResume(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResumeResponse.from(resume));
    }

    // GET /api/resumes
    @GetMapping
    @Operation(summary = "List all uploaded resumes for the current user")
    public ResponseEntity<List<ResumeResponse>> getResumes(@AuthenticationPrincipal User user) {
        List<ResumeResponse> resumes = resumeService.getUserResumes(user)
                .stream()
                .map(ResumeResponse::from)
                .toList();
        return ResponseEntity.ok(resumes);
    }

    // GET /api/resumes/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get a single resume by ID")
    public ResponseEntity<ResumeResponse> getResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Resume resume = resumeService.getResume(id, user);
        return ResponseEntity.ok(ResumeResponse.from(resume));
    }

    // DELETE /api/resumes/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a resume and its file")
    public ResponseEntity<Void> deleteResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        resumeService.deleteResume(id, user);
        return ResponseEntity.noContent().build();
    }
}