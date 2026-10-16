package com.eav02.backend.moderation.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.eav02.backend.moderation.dto.AdminReportPageResponse;
import com.eav02.backend.moderation.dto.AdminReportResponse;
import com.eav02.backend.moderation.service.ModerationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/admin/reports")
public class ModerationController {

    private final ModerationService moderation;

    public ModerationController(ModerationService moderation) {
        this.moderation = moderation;
    }

    @GetMapping
    public AdminReportPageResponse pending(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return moderation.pending(page, size);
    }

    @PatchMapping("/{reportId}/review")
    public AdminReportResponse review(@PathVariable UUID reportId, @AuthenticationPrincipal Jwt jwt) {
        return moderation.review(reportId, adminId(jwt));
    }

    @PatchMapping("/{reportId}/restore")
    public AdminReportResponse restore(@PathVariable UUID reportId, @AuthenticationPrincipal Jwt jwt) {
        return moderation.restore(reportId, adminId(jwt));
    }

    @PatchMapping("/{reportId}/remove")
    public AdminReportResponse remove(@PathVariable UUID reportId, @AuthenticationPrincipal Jwt jwt) {
        return moderation.remove(reportId, adminId(jwt));
    }

    @PatchMapping("/{reportId}/dismiss")
    public AdminReportResponse dismiss(@PathVariable UUID reportId, @AuthenticationPrincipal Jwt jwt) {
        return moderation.dismiss(reportId, adminId(jwt));
    }

    private UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
