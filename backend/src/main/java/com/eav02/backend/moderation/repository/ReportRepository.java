package com.eav02.backend.moderation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("select r from Report r join fetch r.reporter where r.status = :status")
    Page<Report> findByStatusWithReporter(@Param("status") ReportStatus status, Pageable pageable);

    @Query("select r from Report r join fetch r.reporter where r.id = :id")
    java.util.Optional<Report> findWithReporterById(@Param("id") UUID id);

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(UUID reporterId, ReportTargetType targetType,
            UUID targetId, ReportStatus status);
}