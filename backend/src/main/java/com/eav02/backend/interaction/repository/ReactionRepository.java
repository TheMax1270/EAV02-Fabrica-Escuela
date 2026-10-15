package com.eav02.backend.interaction.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.eav02.backend.interaction.entity.Reaction;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {
    @Modifying
    @Query("delete from Reaction r where r.post.id = :postId")
    int deletePostReactions(@Param("postId") UUID postId);
    @Modifying
    @Query(value = "INSERT INTO interactions.reactions (id, user_id, project_id, post_id, type, created_at) VALUES (:id, :userId, :projectId, :postId, 'LIKE', :createdAt) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertLike(@Param("id") UUID id, @Param("userId") UUID userId, @Param("projectId") UUID projectId,
            @Param("postId") UUID postId, @Param("createdAt") Instant createdAt);
    long countByProject_Id(UUID id);
    long countByPost_Id(UUID id);
    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);
    boolean existsByPost_IdAndUser_Id(UUID postId, UUID userId);
    long deleteByProject_IdAndUser_Id(UUID projectId, UUID userId);
    long deleteByPost_IdAndUser_Id(UUID postId, UUID userId);
}
