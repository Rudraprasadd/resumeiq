package com.resumeiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AnalysisRequest {

    public record RunAnalysisRequest(
        @NotBlank(message = "Resume ID is required")
        String resumeId,

        @NotBlank(message = "Job description is required")
        @Size(min = 50, message = "Job description must be at least 50 characters")
        String jobDescription,

        String jobTitle,
        String companyName
    ) {}
}