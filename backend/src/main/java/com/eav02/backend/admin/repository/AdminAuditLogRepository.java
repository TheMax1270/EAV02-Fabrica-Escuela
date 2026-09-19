package com.eav02.backend.admin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eav02.backend.admin.entity.AdminAuditLog;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
}
