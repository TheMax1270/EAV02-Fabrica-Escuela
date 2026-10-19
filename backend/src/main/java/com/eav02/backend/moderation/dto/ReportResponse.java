package com.eav02.backend.moderation.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportReason;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;

public record ReportResponse(UUID id, UUID reporterId, String reporterUsername, ReportTargetType targetType,
        UUID targetId, ReportReason reason, String details, ReportStatus status, Instant createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(report.getId(), report.getReporter().getId(),
                report.getReporter().getUsername(), report.getTargetType(), report.getTargetId(),
                report.getReason(), report.getDetails(), report.getStatus(), report.getCreatedAt());
    }
}