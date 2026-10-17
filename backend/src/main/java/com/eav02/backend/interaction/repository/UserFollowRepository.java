package com.eav02.backend.interaction.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.interaction.dto.FollowUserResponse;
import com.eav02.backend.interaction.entity.UserFollow;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    @Modifying
    @Query(value = "INSERT INTO interactions.user_follows (id, follower_id, followed_id, created_at) "
            + "VALUES (:id, :followerId, :followedId, :createdAt) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertFollow(@Param("id") UUID id, @Param("followerId") UUID followerId,
            @Param("followedId") UUID followedId, @Param("createdAt") Instant createdAt);

    long deleteByFollower_IdAndFollowed_Id(UUID followerId, UUID followedId);

    boolean existsByFollower_IdAndFollowed_Id(UUID followerId, UUID followedId);

    long countByFollowed_Id(UUID followedId);

    long countByFollower_Id(UUID followerId);

    @Query(value = "select new com.eav02.backend.interaction.dto.FollowUserResponse(u.id, u.username, f.createdAt) "
            + "from UserFollow f join f.follower u where f.followed.id = :userId and u.enabled = true",
            countQuery = "select count(f) from UserFollow f join f.follower u "
                    + "where f.followed.id = :userId and u.enabled = true")
    Page<FollowUserResponse> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "select new com.eav02.backend.interaction.dto.FollowUserResponse(u.id, u.username, f.createdAt) "
            + "from UserFollow f join f.followed u where f.follower.id = :userId and u.enabled = true",
            countQuery = "select count(f) from UserFollow f join f.followed u "
                    + "where f.follower.id = :userId and u.enabled = true")
    Page<FollowUserResponse> findFollowing(@Param("userId") UUID userId, Pageable pageable);
}
