package com.eav02.backend.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.notification.entity.Notification;
import com.eav02.backend.notification.entity.NotificationTargetType;
import com.eav02.backend.notification.entity.NotificationType;

public record NotificationResponse(UUID id, NotificationType type, UUID actorId, String actorUsername,
        NotificationTargetType targetType, UUID targetId, String targetLabel, Instant createdAt, boolean read) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getActor().getId(), notification.getActor().getUsername(),
                notification.getTargetType(), notification.getTargetId(), notification.getTargetLabel(),
                notification.getCreatedAt(), notification.isRead());
    }
}