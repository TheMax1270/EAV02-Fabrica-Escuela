package com.eav02.backend.interaction.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.eav02.backend.common.exception.SelfFollowException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.dto.FollowUserResponse;
import com.eav02.backend.interaction.repository.UserFollowRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    UserFollowRepository follows;
    @Mock
    UserRepository users;

    FollowService service;
    final UUID follower = UUID.randomUUID();
    final UUID followed = UUID.randomUUID();
    final Instant now = Instant.parse("2026-10-17T12:00:00Z");

    @BeforeEach
    void setup() {
        service = new FollowService(follows, users, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void followCreatesRelationAndRepeatingIsIdempotent() {
        when(users.findById(followed)).thenReturn(Optional.of(user(true)));
        when(follows.insertFollow(any(), eq(follower), eq(followed), eq(now))).thenReturn(1, 0);

        assertDoesNotThrow(() -> service.follow(follower, followed));
        assertDoesNotThrow(() -> service.follow(follower, followed));

        verify(follows, times(2)).insertFollow(any(), eq(follower), eq(followed), eq(now));
    }

    @Test
    void followingOwnAccountIsRejectedWithoutTouchingRepositories() {
        assertThrows(SelfFollowException.class, () -> service.follow(follower, follower));
        verifyNoInteractions(follows, users);
    }

    @Test
    void followingMissingUserIsRejected() {
        when(users.findById(followed)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.follow(follower, followed));
        verifyNoInteractions(follows);
    }

    @Test
    void followingSuspendedUserIsRejected() {
        when(users.findById(followed)).thenReturn(Optional.of(user(false)));
        assertThrows(UserNotFoundException.class, () -> service.follow(follower, followed));
        verifyNoInteractions(follows);
    }

    @Test
    void unfollowRemovesRelationEvenWhenMissing() {
        when(users.existsById(followed)).thenReturn(true);
        when(follows.deleteByFollower_IdAndFollowed_Id(follower, followed)).thenReturn(1L, 0L);

        assertDoesNotThrow(() -> service.unfollow(follower, followed));
        assertDoesNotThrow(() -> service.unfollow(follower, followed));

        verify(follows, times(2)).deleteByFollower_IdAndFollowed_Id(follower, followed);
    }

    @Test
    void unfollowMissingUserIsRejected() {
        assertThrows(UserNotFoundException.class, () -> service.unfollow(follower, followed));
        verifyNoInteractions(follows);
    }

    @Test
    void anonymousSummaryHasCountsWithoutPersonalState() {
        when(users.existsById(followed)).thenReturn(true);
        when(follows.countByFollowed_Id(followed)).thenReturn(3L);
        when(follows.countByFollower_Id(followed)).thenReturn(5L);

        var result = service.summary(followed, null);

        assertEquals(3, result.followersCount());
        assertEquals(5, result.followingCount());
        assertNull(result.followedByMe());
        verify(follows, never()).existsByFollower_IdAndFollowed_Id(any(), any());
    }

    @Test
    void authenticatedSummaryReportsFollowState() {
        when(users.existsById(followed)).thenReturn(true);
        when(follows.existsByFollower_IdAndFollowed_Id(follower, followed)).thenReturn(true);

        assertTrue(service.summary(followed, follower).followedByMe());
    }

    @Test
    void listsFollowersNewestFirst() {
        when(users.existsById(followed)).thenReturn(true);
        var entry = new FollowUserResponse(follower, "ada", now);
        when(follows.findFollowers(eq(followed), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(entry), invocation.getArgument(1, Pageable.class), 1));

        var result = service.followers(followed, 0, 20);

        assertEquals(List.of(entry), result.content());
        assertEquals(1, result.totalElements());
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(follows).findFollowers(eq(followed), captor.capture());
        assertEquals(PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))),
                captor.getValue());
    }

    @Test
    void listsFollowing() {
        when(users.existsById(follower)).thenReturn(true);
        var entry = new FollowUserResponse(followed, "grace", now);
        when(follows.findFollowing(eq(follower), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(entry), invocation.getArgument(1, Pageable.class), 1));

        assertEquals(List.of(entry), service.following(follower, 0, 20).content());
    }

    @Test
    void listingMissingUserIsRejected() {
        assertThrows(UserNotFoundException.class, () -> service.followers(followed, 0, 20));
        assertThrows(UserNotFoundException.class, () -> service.following(followed, 0, 20));
        assertThrows(UserNotFoundException.class, () -> service.summary(followed, null));
        verifyNoInteractions(follows);
    }

    private AppUser user(boolean enabled) {
        return new AppUser("Grace Hopper", "grace", "grace@example.com", "hash", UserRole.DEVELOPER, enabled, now);
    }
}
