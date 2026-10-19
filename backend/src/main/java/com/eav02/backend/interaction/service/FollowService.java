package com.eav02.backend.interaction.service;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.SelfFollowException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.dto.FollowPageResponse;
import com.eav02.backend.interaction.dto.FollowSummaryResponse;
import com.eav02.backend.interaction.dto.FollowedIdsResponse;
import com.eav02.backend.interaction.repository.UserFollowRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class FollowService {

    private final UserFollowRepository follows;
    private final UserRepository users;
    private final Clock clock;

    public FollowService(UserFollowRepository follows, UserRepository users, Clock clock) {
        this.follows = follows;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void follow(UUID followerId, UUID followedId) {
        if (followerId.equals(followedId)) {
            throw new SelfFollowException();
        }
        requireActiveUser(followedId);
        follows.insertFollow(UUID.randomUUID(), followerId, followedId, clock.instant());
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followedId) {
        requireExistingUser(followedId);
        follows.deleteByFollower_IdAndFollowed_Id(followerId, followedId);
    }

    @Transactional(readOnly = true)
    public FollowSummaryResponse summary(UUID userId, UUID viewerId) {
        requireExistingUser(userId);
        Boolean followedByMe = viewerId == null ? null : follows.existsByFollower_IdAndFollowed_Id(viewerId, userId);
        return new FollowSummaryResponse(follows.countByFollowed_Id(userId), follows.countByFollower_Id(userId),
                followedByMe);
    }

    @Transactional(readOnly = true)
    public FollowPageResponse followers(UUID userId, int page, int size) {
        requireExistingUser(userId);
        return FollowPageResponse.from(follows.findFollowers(userId, pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public FollowPageResponse following(UUID userId, int page, int size) {
        requireExistingUser(userId);
        return FollowPageResponse.from(follows.findFollowing(userId, pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public FollowedIdsResponse followedAmong(UUID followerId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return new FollowedIdsResponse(List.of());
        }
        return new FollowedIdsResponse(follows.findFollowedIds(followerId, userIds));
    }

    private void requireExistingUser(UUID userId) {
        if (!users.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

    private void requireActiveUser(UUID userId) {
        users.findById(userId).filter(AppUser::isEnabled).orElseThrow(UserNotFoundException::new);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    }
}
