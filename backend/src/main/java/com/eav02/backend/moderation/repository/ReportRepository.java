package com.eav02.backend.moderation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(UUID reporterId, ReportTargetType targetType,
            UUID targetId, ReportStatus status);
}