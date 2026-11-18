package com.eav02.backend.interaction.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.eav02.backend.interaction.repository.UserBlockRepository;
import com.eav02.backend.interaction.repository.UserFollowRepository;
import com.eav02.backend.notification.service.NotificationService;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FollowServiceNotificationTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    @Mock UserFollowRepository follows;
    @Mock UserRepository users;
    @Mock UserBlockRepository blocks;
    @Mock NotificationService notifications;

    private final UUID followerId = UUID.randomUUID();
    private final UUID followedId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AppUser followed = new AppUser("Linus", "linus", "linus@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        when(users.findById(followedId)).thenReturn(Optional.of(followed));
    }

    @Test
    void notifiesTheFollowedUserWhenTheRelationIsCreated() {
        FollowService service = new FollowService(follows, users, Clock.fixed(NOW, ZoneOffset.UTC), blocks, notifications);
        when(follows.insertFollow(any(), eq(followerId), eq(followedId), eq(NOW))).thenReturn(1);

        service.follow(followerId, followedId);

        verify(notifications).newFollower(followerId, followedId);
    }

    @Test
    void doesNotNotifyAgainWhenTheUserWasAlreadyFollowed() {
        FollowService service = new FollowService(follows, users, Clock.fixed(NOW, ZoneOffset.UTC), blocks, notifications);
        when(follows.insertFollow(any(), eq(followerId), eq(followedId), eq(NOW))).thenReturn(0);

        service.follow(followerId, followedId);

        verifyNoInteractions(notifications);
    }

    @Test
    void worksWithoutNotificationsWhenBuiltWithTheLegacyConstructor() {
        FollowService legacy = new FollowService(follows, users, Clock.fixed(NOW, ZoneOffset.UTC), blocks);
        when(follows.insertFollow(any(), eq(followerId), eq(followedId), eq(NOW))).thenReturn(1);

        legacy.follow(followerId, followedId);

        verifyNoInteractions(notifications);
    }
}