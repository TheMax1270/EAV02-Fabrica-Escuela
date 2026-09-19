package com.eav02.backend.admin.service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.entity.AdminAuditLog;
import com.eav02.backend.admin.repository.AdminAuditLogRepository;
import com.eav02.backend.user.entity.AppUser;

@Service
public class AdminAuditService {

    public static final String TARGET_USER = "USER";

    private final AdminAuditLogRepository logs;
    private final Clock clock;

    public AdminAuditService(AdminAuditLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @Transactional
    public AdminAuditLog record(AppUser admin, AdminAction action, String targetType, UUID targetId,
            Map<String, Object> details) {
        var entry = new AdminAuditLog(UUID.randomUUID(), admin, action, targetType, targetId, details,
                clock.instant());
        return logs.save(entry);
    }
}
