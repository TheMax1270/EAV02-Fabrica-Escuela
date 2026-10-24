package com.eav02.backend.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.entity.AdminAuditLog;
import com.eav02.backend.admin.repository.AdminAuditLogRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AdminAuditLogRepository logs;

    @Test
    void recordsAnAuditEntryWithTheGivenDetails() {
        var service = new AdminAuditService(logs, Clock.fixed(NOW, ZoneOffset.UTC));
        var admin = new AppUser("Grace Hopper", "grace", "grace@example.com", "hash",
                UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        UUID targetId = UUID.randomUUID();
        Map<String, Object> details = Map.of("previousStatus", "ACTIVE", "newStatus", "SUSPENDED");
        when(logs.save(any(AdminAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.record(admin, AdminAction.USER_SUSPENDED, AdminAuditService.TARGET_USER,
                targetId, details);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(logs).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertEquals(admin, saved.getAdmin());
        assertEquals("USER_SUSPENDED", saved.getAction());
        assertEquals(AdminAuditService.TARGET_USER, saved.getTargetType());
        assertEquals(targetId, saved.getTargetId());
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals(result, saved);
    }
}