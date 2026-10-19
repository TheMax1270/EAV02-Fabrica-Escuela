package com.eav02.backend.moderation.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.moderation.entity.ReportReason;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;

public record AdminReportResponse(UUID id, UUID reporterId, String reporterUsername,
        ReportTargetType targetType, UUID targetId, String targetPreview, String targetUsername,
        ReportReason reason, String details, ReportStatus status, ModerationStatus moderationStatus,
        Instant createdAt) {
}
