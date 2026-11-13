package com.eav02.backend.notification.service;

import static com.eav02.backend.notification.NotificationTestData.notification;
import static com.eav02.backend.notification.NotificationTestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.eav02.backend.common.exception.NotificationNotFoundException;
import com.eav02.backend.notification.entity.Notification;
import com.eav02.backend.notification.entity.NotificationTargetType;
import com.eav02.backend.notification.entity.NotificationType;
import com.eav02.backend.notification.repository.NotificationRepository;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.project.entity.Project;

class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID ownerId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Test
    void commentOnProjectNotifiesTheOwnerWithTheProjectTitle() {
        service.commentOnProject(project("TaskFlow"), actorId);

        verify(repository).insert(eq(ownerId), eq(actorId), eq("COMMENT"), eq("PROJECT"), eq(targetId),
                eq("TaskFlow"), eq(NOW));
    }

    @Test
    void likeOnProjectNotifiesTheOwner() {
        service.likeOnProject(project("TaskFlow"), actorId);

        verify(repository).insert(eq(ownerId), eq(actorId), eq("LIKE"), eq("PROJECT"), eq(targetId),
                eq("TaskFlow"), eq(NOW));
    }

    @Test
    void commentOnPostUsesTheTitleAsLabel() {
        service.commentOnPost(post("Mi discusion", "contenido"), actorId);

        verify(repository).insert(eq(ownerId), eq(actorId), eq("COMMENT"), eq("POST"), eq(targetId),
                eq("Mi discusion"), eq(NOW));
    }

    @Test
    void likeOnPostNotifiesTheOwner() {
        service.likeOnPost(post("Mi discusion", "contenido"), actorId);

        verify(repository).insert(eq(ownerId), eq(actorId), eq("LIKE"), eq("POST"), eq(targetId),
                eq("Mi discusion"), eq(NOW));
    }

    @Test
    void newFollowerNotifiesTheFollowedUser() {
        UUID follower = UUID.randomUUID();
        UUID followed = UUID.randomUUID();

        service.newFollower(follower, followed);

        verify(repository).insert(eq(followed), eq(follower), eq("FOLLOWER"), eq("USER"), eq(followed),
                isNull(), eq(NOW));
    }

    @Test
    void newMessageNotifiesTheRecipient() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        service.newMessage(sender, recipient);

        verify(repository).insert(eq(recipient), eq(sender), eq("MESSAGE"), eq("USER"), eq(recipient),
                isNull(), eq(NOW));
    }

    @Test
    void doesNotNotifyTheUserAboutTheirOwnActions() {
        service.newMessage(ownerId, ownerId);
        service.newFollower(ownerId, ownerId);
        service.likeOnProject(project("TaskFlow"), ownerId);
        service.commentOnPost(post("Titulo", "contenido"), ownerId);

        verifyNoInteractions(repository);
    }

    @Test
    void projectWithoutTitleProducesNoLabel() {
        service.commentOnProject(project(null), actorId);

        assertThat(capturedLabel()).isNull();
    }

    @Test
    void usesTheContentAsLabelWhenThePostHasNoTitle() {
        service.likeOnPost(post(null, "  Primera   linea\ncon espacios  "), actorId);

        assertThat(capturedLabel()).isEqualTo("Primera linea con espacios");
    }

    @Test
    void truncatesLongLabelsToOneHundredCharacters() {
        service.likeOnPost(post(null, "a".repeat(300)), actorId);

        assertThat(capturedLabel()).hasSize(100).endsWith("...");
    }

    @Test
    void blankContentProducesNoLabel() {
        service.likeOnPost(post(null, "   "), actorId);

        assertThat(capturedLabel()).isNull();
    }

    @Test
    void listReturnsTheNotificationsOfTheUserNewestFirst() {
        Notification stored = notification(user(ownerId, "owner"), user(actorId, "ada"), NotificationType.COMMENT,
                NotificationTargetType.PROJECT, targetId, "TaskFlow", NOW);
        when(repository.findForRecipient(eq(ownerId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stored), PageRequest.of(0, 20), 1));

        var page = service.list(ownerId, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findForRecipient(eq(ownerId), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).actorUsername()).isEqualTo("ada");
        assertThat(page.content().get(0).read()).isFalse();
    }

    @Test
    void unreadCountReturnsTheRepositoryCount() {
        when(repository.countByRecipient_IdAndReadAtIsNull(ownerId)).thenReturn(3L);

        assertThat(service.unreadCount(ownerId).count()).isEqualTo(3);
    }

    @Test
    void markReadStoresTheReadDate() {
        Notification stored = notification(user(ownerId, "owner"), user(actorId, "ada"), NotificationType.LIKE,
                NotificationTargetType.POST, targetId, "Post", NOW.minusSeconds(60));
        when(repository.findOwnedById(stored.getId(), ownerId)).thenReturn(Optional.of(stored));

        var response = service.markRead(stored.getId(), ownerId);

        assertThat(response.read()).isTrue();
        assertThat(stored.getReadAt()).isEqualTo(NOW);
    }

    @Test
    void markReadFailsWhenTheNotificationDoesNotBelongToTheUser() {
        UUID missing = UUID.randomUUID();
        when(repository.findOwnedById(missing, ownerId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> service.markRead(missing, ownerId));
    }

    @Test
    void markAllReadUpdatesEveryUnreadNotificationOfTheUser() {
        service.markAllRead(ownerId);

        verify(repository).markAllRead(ownerId, NOW);
    }

    private String capturedLabel() {
        ArgumentCaptor<String> label = ArgumentCaptor.forClass(String.class);
        verify(repository).insert(any(), any(), any(), any(), any(), label.capture(), any());
        return label.getValue();
    }

    private Project project(String title) {
        Project project = mock(Project.class);
        when(project.getUser()).thenReturn(user(ownerId, "owner"));
        when(project.getId()).thenReturn(targetId);
        when(project.getTitle()).thenReturn(title);
        return project;
    }

    private Post post(String title, String content) {
        Post post = mock(Post.class);
        when(post.getUser()).thenReturn(user(ownerId, "owner"));
        when(post.getId()).thenReturn(targetId);
        when(post.getTitle()).thenReturn(title);
        when(post.getContent()).thenReturn(content);
        return post;
    }
}