package com.eav02.backend.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.eav02.backend.notification.dto.NotificationPageResponse;
import com.eav02.backend.notification.dto.NotificationResponse;
import com.eav02.backend.notification.dto.UnreadCountResponse;
import com.eav02.backend.notification.entity.NotificationTargetType;
import com.eav02.backend.notification.entity.NotificationType;
import com.eav02.backend.notification.service.NotificationService;

class NotificationControllerTest {

    private final NotificationService service = mock(NotificationService.class);
    private final NotificationController controller = new NotificationController(service);
    private final UUID userId = UUID.randomUUID();
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();

    @Test
    void listReturnsThePageWithoutCaching() {
        var page = new NotificationPageResponse(List.of(), 0, 20, 0, 0);
        when(service.list(userId, 0, 20)).thenReturn(page);

        var response = controller.list(jwt, 0, 20);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(page);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void unreadCountReturnsTheCountWithoutCaching() {
        when(service.unreadCount(userId)).thenReturn(new UnreadCountResponse(5));

        var response = controller.unreadCount(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().count()).isEqualTo(5);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void markReadDelegatesWithTheAuthenticatedUser() {
        UUID notificationId = UUID.randomUUID();
        var updated = new NotificationResponse(notificationId, NotificationType.LIKE, UUID.randomUUID(), "ada",
                NotificationTargetType.POST, UUID.randomUUID(), "Post", Instant.parse("2026-11-20T10:00:00Z"), true);
        when(service.markRead(notificationId, userId)).thenReturn(updated);

        assertThat(controller.markRead(notificationId, jwt)).isSameAs(updated);
    }

    @Test
    void markAllReadAnswersNoContent() {
        var response = controller.markAllRead(jwt);

        verify(service).markAllRead(userId);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}