package com.eav02.backend.notification.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.notification.dto.NotificationPageResponse;
import com.eav02.backend.notification.dto.NotificationResponse;
import com.eav02.backend.notification.dto.UnreadCountResponse;
import com.eav02.backend.notification.service.NotificationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public ResponseEntity<NotificationPageResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(notifications.list(userId(jwt), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(notifications.unreadCount(userId(jwt)));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable UUID notificationId, @AuthenticationPrincipal Jwt jwt) {
        return notifications.markRead(notificationId, userId(jwt));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notifications.markAllRead(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}