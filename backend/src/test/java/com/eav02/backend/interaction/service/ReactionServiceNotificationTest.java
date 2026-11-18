package com.eav02.backend.interaction.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eav02.backend.interaction.repository.ReactionRepository;
import com.eav02.backend.notification.service.NotificationService;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReactionServiceNotificationTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    @Mock ReactionRepository reactions;
    @Mock ProjectRepository projects;
    @Mock PostRepository posts;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock Project project;
    @Mock Post post;

    private ReactionService service;
    private final UUID targetId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReactionService(reactions, projects, posts, users, Clock.fixed(NOW, ZoneOffset.UTC), notifications);
    }

    @Test
    void likeOnAProjectNotifiesItsOwner() {
        when(projects.existsById(targetId)).thenReturn(true);
        when(users.existsById(actorId)).thenReturn(true);
        when(reactions.insertLike(any(), eq(actorId), eq(targetId), isNull(), eq(NOW))).thenReturn(1);
        when(projects.findWithUserById(targetId)).thenReturn(Optional.of(project));

        service.like(targetId, true, actorId);

        verify(notifications).likeOnProject(project, actorId);
    }

    @Test
    void likeOnAPostNotifiesItsOwner() {
        when(posts.existsById(targetId)).thenReturn(true);
        when(users.existsById(actorId)).thenReturn(true);
        when(reactions.insertLike(any(), eq(actorId), isNull(), eq(targetId), eq(NOW))).thenReturn(1);
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post));

        service.like(targetId, false, actorId);

        verify(notifications).likeOnPost(post, actorId);
    }

    @Test
    void repeatedLikeDoesNotNotifyAgain() {
        when(projects.existsById(targetId)).thenReturn(true);
        when(users.existsById(actorId)).thenReturn(true);
        when(reactions.insertLike(any(), eq(actorId), eq(targetId), isNull(), eq(NOW))).thenReturn(0);

        service.like(targetId, true, actorId);

        verifyNoInteractions(notifications);
    }
}