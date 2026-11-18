package com.eav02.backend.interaction.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.SelfBlockException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.repository.UserBlockRepository;
import com.eav02.backend.interaction.repository.UserFollowRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    @Mock UserBlockRepository blocks;
    @Mock UserFollowRepository follows;
    @Mock UserRepository users;

    private BlockService service;
    private UUID blockerId;
    private UUID blockedId;

    @BeforeEach
    void setUp() {
        service = new BlockService(blocks, follows, users, Clock.fixed(NOW, ZoneOffset.UTC));
        blockerId = UUID.randomUUID();
        blockedId = UUID.randomUUID();
    }

    @Test
    void blocksAnotherUserAndRemovesBothFollowDirections() {
        when(users.findById(blockedId)).thenReturn(Optional.of(user(blockedId, true)));

        service.block(blockerId, blockedId);

        verify(blocks).insertBlock(any(), eq(blockerId), eq(blockedId), eq(NOW));
        verify(follows).deleteByFollower_IdAndFollowed_Id(blockerId, blockedId);
        verify(follows).deleteByFollower_IdAndFollowed_Id(blockedId, blockerId);
    }

    @Test
    void rejectsBlockingSelfWithoutWriting() {
        assertThrows(SelfBlockException.class, () -> service.block(blockerId, blockerId));
        verifyNoInteractions(blocks, follows, users);
    }

    @Test
    void rejectsBlockingMissingOrDisabledUser() {
        when(users.findById(blockedId)).thenReturn(Optional.of(user(blockedId, false)));
        assertThrows(UserNotFoundException.class, () -> service.block(blockerId, blockedId));
        verifyNoInteractions(blocks, follows);
    }

    @Test
    void unblockRemovesOnlyBlockCreatedByCurrentUser() {
        when(users.existsById(blockedId)).thenReturn(true);

        service.unblock(blockerId, blockedId);

        verify(blocks).deleteByBlocker_IdAndBlocked_Id(blockerId, blockedId);
        verify(follows, never()).deleteByFollower_IdAndFollowed_Id(any(), any());
    }

    private AppUser user(UUID id, boolean enabled) {
        AppUser user = new AppUser("User", "user" + id.toString().substring(0, 6),
                id + "@example.com", "hash", UserRole.DEVELOPER, enabled, NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
