package com.eav02.backend.interaction.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.SelfBlockException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.dto.BlockSummaryResponse;
import com.eav02.backend.interaction.repository.UserBlockRepository;
import com.eav02.backend.interaction.repository.UserFollowRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class BlockService {

    private final UserBlockRepository blocks;
    private final UserFollowRepository follows;
    private final UserRepository users;
    private final Clock clock;

    public BlockService(UserBlockRepository blocks, UserFollowRepository follows,
            UserRepository users, Clock clock) {
        this.blocks = blocks;
        this.follows = follows;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) throw new SelfBlockException();
        requireActiveUser(blockedId);
        blocks.insertBlock(UUID.randomUUID(), blockerId, blockedId, clock.instant());
        follows.deleteByFollower_IdAndFollowed_Id(blockerId, blockedId);
        follows.deleteByFollower_IdAndFollowed_Id(blockedId, blockerId);
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        requireExistingUser(blockedId);
        blocks.deleteByBlocker_IdAndBlocked_Id(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public BlockSummaryResponse summary(UUID userId, UUID viewerId) {
        requireExistingUser(userId);
        if (viewerId == null) return new BlockSummaryResponse(false, false);
        return new BlockSummaryResponse(blocks.existsByBlocker_IdAndBlocked_Id(viewerId, userId),
                blocks.existsByBlocker_IdAndBlocked_Id(userId, viewerId));
    }

    private void requireExistingUser(UUID userId) {
        if (!users.existsById(userId)) throw new UserNotFoundException();
    }

    private void requireActiveUser(UUID userId) {
        users.findById(userId).filter(AppUser::isEnabled).orElseThrow(UserNotFoundException::new);
    }
}
