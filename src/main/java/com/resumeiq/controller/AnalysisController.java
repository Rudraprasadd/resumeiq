package com.resumeiq.controller;

import com.resumeiq.dto.request.AnalysisRequest.RunAnalysisRequest;
import com.resumeiq.dto.response.ResumeAnalysisResponse.*;
import com.resumeiq.model.User;
import com.resumeiq.service.AnalysisService;
import com.resumeiq.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
@Tag(name = "Analyses", description = "Run AI resume analysis against a job description")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UsageService usageService;

    public AnalysisController(AnalysisService analysisService, UsageService usageService) {
        this.analysisService = analysisService;
        this.usageService    = usageService;
    }

    // POST /api/analyses
    // This is the CORE endpoint — takes resumeId + JD, calls Claude, returns full analysis
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Run AI analysis",
        description = "Compares your resume against the job description using Claude AI. " +
                      "Returns match score, ATS score, missing keywords, rewrite suggestions, " +
                      "cover letter, and interview questions. FREE users: 3/month. PRO: unlimited."
    )
    public ResponseEntity<AnalysisResponse> runAnalysis(
            @Valid @RequestBody RunAnalysisRequest request,
            @AuthenticationPrincipal User user) {

        var analysis = analysisService.runAnalysis(
            UUID.fromString(request.resumeId()),
            request.jobDescription(),
            request.jobTitle(),
            request.companyName(),
            user
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AnalysisResponse.from(analysis));
    }

    // GET /api/analyses
    @GetMapping
    @Operation(summary = "Get analysis history for the current user")
    public ResponseEntity<List<AnalysisSummary>> getHistory(@AuthenticationPrincipal User user) {
        List<AnalysisSummary> history = analysisService.getUserAnalyses(user)
                .stream()
                .map(AnalysisSummary::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    // GET /api/analyses/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get full details of a single analysis")
    public ResponseEntity<AnalysisResponse> getAnalysis(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(AnalysisResponse.from(analysisService.getAnalysis(id, user)));
    }

    // GET /api/analyses/quota
    @GetMapping("/quota")
    @Operation(summary = "Check remaining analyses quota for this month")
    public ResponseEntity<QuotaResponse> getQuota(@AuthenticationPrincipal User user) {
        long used      = usageService.getUsageThisMonth(user);
        long remaining = usageService.getRemainingThisMonth(user);
        boolean isPro  = user.getPlan() == com.resumeiq.model.User.Plan.PRO;

        return ResponseEntity.ok(new QuotaResponse(
            user.getPlan().name(),
            used,
            isPro ? -1 : remaining,   // -1 = unlimited for PRO
            isPro ? -1 : 3
        ));
    }
}