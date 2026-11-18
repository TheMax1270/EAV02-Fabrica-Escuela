package com.eav02.backend.notification.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.NotificationNotFoundException;
import com.eav02.backend.notification.dto.NotificationPageResponse;
import com.eav02.backend.notification.dto.NotificationResponse;
import com.eav02.backend.notification.dto.UnreadCountResponse;
import com.eav02.backend.notification.entity.NotificationTargetType;
import com.eav02.backend.notification.entity.NotificationType;
import com.eav02.backend.notification.repository.NotificationRepository;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.project.entity.Project;

@Service
public class NotificationService {

    private static final int LABEL_MAX = 100;

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public void commentOnProject(Project project, UUID actorId) {
        create(project.getUser().getId(), actorId, NotificationType.COMMENT, NotificationTargetType.PROJECT,
                project.getId(), project.getTitle());
    }

    @Transactional
    public void commentOnPost(Post post, UUID actorId) {
        create(post.getUser().getId(), actorId, NotificationType.COMMENT, NotificationTargetType.POST,
                post.getId(), postLabel(post));
    }

    @Transactional
    public void likeOnProject(Project project, UUID actorId) {
        create(project.getUser().getId(), actorId, NotificationType.LIKE, NotificationTargetType.PROJECT,
                project.getId(), project.getTitle());
    }

    @Transactional
    public void likeOnPost(Post post, UUID actorId) {
        create(post.getUser().getId(), actorId, NotificationType.LIKE, NotificationTargetType.POST,
                post.getId(), postLabel(post));
    }

    // En seguidor y mensaje el destino es el propio usuario notificado.
    @Transactional
    public void newFollower(UUID followerId, UUID followedId) {
        create(followedId, followerId, NotificationType.FOLLOWER, NotificationTargetType.USER, followedId, null);
    }

    @Transactional
    public void newMessage(UUID senderId, UUID recipientId) {
        create(recipientId, senderId, NotificationType.MESSAGE, NotificationTargetType.USER, recipientId, null);
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return NotificationPageResponse.from(notifications.findForRecipient(userId, pageable));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notifications.countByRecipient_IdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        var notification = notifications.findOwnedById(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notifications.markAllRead(userId, clock.instant());
    }

    private void create(UUID recipientId, UUID actorId, NotificationType type, NotificationTargetType targetType,
            UUID targetId, String label) {
        if (recipientId.equals(actorId)) {
            return;
        }
        notifications.insert(recipientId, actorId, type.name(), targetType.name(), targetId,
            abbreviate(label), clock.instant());
    }

    private static String postLabel(Post post) {
        return post.getTitle() != null ? post.getTitle() : post.getContent();
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String text = value.strip().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            return null;
        }
        return text.length() <= LABEL_MAX ? text : text.substring(0, LABEL_MAX - 3) + "...";
    }
}