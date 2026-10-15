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
    @Query("select c from Comment c join fetch c.user left join fetch c.project left join fetch c.post left join fetch c.parent where c.id = :id")
    Optional<Comment> findWithTargetById(@Param("id") UUID id);

    @Query(value = "select c from Comment c join fetch c.user where c.project.id = :id and c.parent is null",
            countQuery = "select count(c) from Comment c where c.project.id = :id and c.parent is null")
    Page<Comment> findProjectRoots(@Param("id") UUID id, Pageable pageable);

    @Query(value = "select c from Comment c join fetch c.user where c.post.id = :id and c.parent is null",
            countQuery = "select count(c) from Comment c where c.post.id = :id and c.parent is null")
    Page<Comment> findPostRoots(@Param("id") UUID id, Pageable pageable);

    @Query(value = "select c from Comment c join fetch c.user where c.parent.id = :id",
            countQuery = "select count(c) from Comment c where c.parent.id = :id")
    Page<Comment> findReplies(@Param("id") UUID id, Pageable pageable);

    interface ReplyCount {
        UUID getParentId();
        long getTotal();
    }
    @Query("select c.parent.id as parentId, count(c) as total from Comment c where c.parent.id in :ids group by c.parent.id")
    List<ReplyCount> countReplies(@Param("ids") List<UUID> ids);
}
