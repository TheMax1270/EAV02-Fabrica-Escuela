package com.eav02.backend.notification.entity;

import static com.eav02.backend.notification.NotificationTestData.notification;
import static com.eav02.backend.notification.NotificationTestData.user;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.eav02.backend.notification.dto.NotificationPageResponse;
import com.eav02.backend.notification.dto.NotificationResponse;
import com.eav02.backend.notification.dto.UnreadCountResponse;
import com.eav02.backend.user.entity.AppUser;

class NotificationTest {

    private static final Instant CREATED = Instant.parse("2026-11-20T09:00:00Z");
    private static final Instant READ = Instant.parse("2026-11-20T10:00:00Z");

    private final AppUser recipient = user(UUID.randomUUID(), "owner");
    private final AppUser actor = user(UUID.randomUUID(), "ada");
    private final UUID targetId = UUID.randomUUID();

    private Notification stored() {
        return notification(recipient, actor, NotificationType.COMMENT, NotificationTargetType.PROJECT,
                targetId, "TaskFlow", CREATED);
    }

    @Test
    void startsUnreadAndKeepsTheFirstReadDate() {
        Notification notification = stored();
        assertThat(notification.isRead()).isFalse();

        notification.markRead(READ);
        notification.markRead(READ.plusSeconds(60));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(READ);
    }

    @Test
    void exposesTheStoredFields() {
        Notification notification = stored();

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getActor()).isSameAs(actor);
        assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.PROJECT);
        assertThat(notification.getTargetId()).isEqualTo(targetId);
        assertThat(notification.getTargetLabel()).isEqualTo("TaskFlow");
        assertThat(notification.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void responseCopiesEveryField() {
        Notification notification = stored();
        notification.markRead(READ);

        NotificationResponse response = NotificationResponse.from(notification);

        assertThat(response.id()).isEqualTo(notification.getId());
        assertThat(response.type()).isEqualTo(NotificationType.COMMENT);
        assertThat(response.actorId()).isEqualTo(actor.getId());
        assertThat(response.actorUsername()).isEqualTo("ada");
        assertThat(response.targetType()).isEqualTo(NotificationTargetType.PROJECT);
        assertThat(response.targetId()).isEqualTo(targetId);
        assertThat(response.targetLabel()).isEqualTo("TaskFlow");
        assertThat(response.createdAt()).isEqualTo(CREATED);
        assertThat(response.read()).isTrue();
    }

    @Test
    void pageResponseCopiesPagingData() {
        var page = new PageImpl<>(List.of(stored()), PageRequest.of(1, 5), 11);

        NotificationPageResponse response = NotificationPageResponse.from(page);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void unreadCountResponseExposesTheCount() {
        assertThat(new UnreadCountResponse(4).count()).isEqualTo(4);
    }
}