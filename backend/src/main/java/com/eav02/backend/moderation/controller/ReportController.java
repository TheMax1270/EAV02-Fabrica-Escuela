package com.eav02.backend.moderation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.moderation.dto.ReportRequest;
import com.eav02.backend.moderation.dto.ReportResponse;
import com.eav02.backend.moderation.dto.ReportStatusResponse;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/posts/{id}/reports")
    public ResponseEntity<ReportResponse> post(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.reportPost(id, userId(jwt), request));
    }

    @PostMapping("/comments/{id}/reports")
    public ResponseEntity<ReportResponse> comment(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.reportComment(id, userId(jwt), request));
    }

    @PostMapping("/profiles/{id}/reports")
    public ResponseEntity<ReportResponse> profile(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.reportProfile(id, userId(jwt), request));
    }

    @GetMapping("/posts/{id}/reports/me")
    public ReportStatusResponse postStatus(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reportService.status(ReportTargetType.POST, id, userId(jwt));
    }

    @GetMapping("/comments/{id}/reports/me")
    public ReportStatusResponse commentStatus(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reportService.status(ReportTargetType.COMMENT, id, userId(jwt));
    }

    @GetMapping("/profiles/{id}/reports/me")
    public ReportStatusResponse profileStatus(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reportService.status(ReportTargetType.PROFILE, id, userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}