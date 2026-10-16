package com.eav02.backend.moderation.dto;

import com.eav02.backend.moderation.entity.ReportReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(@NotNull ReportReason reason, @Size(max = 500) String details) {

    public ReportRequest {
        details = details == null || details.isBlank() ? null : details.trim();
    }
}