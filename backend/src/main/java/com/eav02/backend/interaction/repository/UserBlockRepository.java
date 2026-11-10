package com.eav02.backend.interaction.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.interaction.entity.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    @Modifying
    @Query(value = "INSERT INTO interactions.user_blocks (id, blocker_id, blocked_id, created_at) "
            + "VALUES (:id, :blockerId, :blockedId, :createdAt) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertBlock(@Param("id") UUID id, @Param("blockerId") UUID blockerId,
            @Param("blockedId") UUID blockedId, @Param("createdAt") java.time.Instant createdAt);

    long deleteByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);

    boolean existsByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);

    @Query("select count(b) > 0 from UserBlock b where "
            + "(b.blocker.id = :firstId and b.blocked.id = :secondId) "
            + "or (b.blocker.id = :secondId and b.blocked.id = :firstId)")
    boolean existsBetween(@Param("firstId") UUID firstId, @Param("secondId") UUID secondId);
}
