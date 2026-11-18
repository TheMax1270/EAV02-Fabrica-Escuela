package com.eav02.backend.notification;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.notification.entity.Notification;
import com.eav02.backend.notification.entity.NotificationTargetType;
import com.eav02.backend.notification.entity.NotificationType;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

public final class NotificationTestData {

    private NotificationTestData() {
    }

    public static AppUser user(UUID id, String username) {
        AppUser user = new AppUser(username, username, username + "@example.com", "hash", UserRole.DEVELOPER,
                true, Instant.parse("2026-11-01T00:00:00Z"));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Notification notification(AppUser recipient, AppUser actor, NotificationType type,
            NotificationTargetType targetType, UUID targetId, String label, Instant createdAt) {
        Notification notification = BeanUtils.instantiateClass(Notification.class);
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(notification, "recipient", recipient);
        ReflectionTestUtils.setField(notification, "actor", actor);
        ReflectionTestUtils.setField(notification, "type", type);
        ReflectionTestUtils.setField(notification, "targetType", targetType);
        ReflectionTestUtils.setField(notification, "targetId", targetId);
        ReflectionTestUtils.setField(notification, "targetLabel", label);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }
}