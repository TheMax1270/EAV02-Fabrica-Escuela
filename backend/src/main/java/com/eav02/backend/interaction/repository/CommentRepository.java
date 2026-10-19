package com.eav02.backend.interaction.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.eav02.backend.interaction.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is not null")
    int deletePostReplies(@Param("postId") UUID postId);

    @Modifying
    @Query("delete from Comment c where c.post.id = :postId and c.parent is null")
    int deletePostRoots(@Param("postId") UUID postId);

    @Modifying
    @Query("update Comment c set c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.REMOVED where c.post.id = :postId and c.moderationStatus <> com.eav02.backend.moderation.entity.ModerationStatus.REMOVED")
    int removePostComments(@Param("postId") UUID postId);

    @Modifying
    @Query("update Comment c set c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE where c.post.id = :postId and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.REMOVED")
    int restorePostComments(@Param("postId") UUID postId);

    @Query("select count(c) from Comment c where c.post.id = :postId and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    long countVisiblePostComments(@Param("postId") UUID postId);

    @Modifying
    @Query(value = "WITH RECURSIVE descendants AS (SELECT id FROM interactions.comments WHERE id = :commentId UNION ALL SELECT c.id FROM interactions.comments c JOIN descendants d ON c.parent_comment_id = d.id) UPDATE interactions.comments SET moderation_status = 'REMOVED' WHERE id IN (SELECT id FROM descendants)", nativeQuery = true)
    int removeCommentTree(@Param("commentId") UUID commentId);
        @Query("select c from Comment c join fetch c.user left join fetch c.project left join fetch c.post left join fetch c.parent where c.id = :id")
    Optional<Comment> findWithTargetById(@Param("id") UUID id);

    @Query(value = "select c from Comment c join fetch c.user where c.project.id = :id and c.parent is null and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE",
            countQuery = "select count(c) from Comment c where c.project.id = :id and c.parent is null and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    Page<Comment> findProjectRoots(@Param("id") UUID id, Pageable pageable);

    @Query(value = "select c from Comment c join fetch c.user where c.post.id = :id and c.parent is null and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE",
            countQuery = "select count(c) from Comment c where c.post.id = :id and c.parent is null and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    Page<Comment> findPostRoots(@Param("id") UUID id, Pageable pageable);

    @Query(value = "select c from Comment c join fetch c.user where c.parent.id = :id and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE",
            countQuery = "select count(c) from Comment c where c.parent.id = :id and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    Page<Comment> findReplies(@Param("id") UUID id, Pageable pageable);

    interface ReplyCount {
        UUID getParentId();
        long getTotal();
    }
    @Query("select c.parent.id as parentId, count(c) as total from Comment c where c.parent.id in :ids and c.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE group by c.parent.id")
    List<ReplyCount> countReplies(@Param("ids") List<UUID> ids);
}
