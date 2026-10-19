package com.eav02.backend.post.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query(value = "select p.* from posts.posts p where p.id = :id for update", nativeQuery = true)
    Optional<Post> findForDeletion(@Param("id") UUID id);

    @Query("select p from Post p join fetch p.user where p.id = :id")
    Optional<Post> findWithUserById(@Param("id") UUID id);

    @Query("select p from Post p join fetch p.user where p.id = :id and p.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    Optional<Post> findPublicWithUserById(@Param("id") UUID id);

        @Query(value = "select p from Post p join fetch p.user where p.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE",
            countQuery = "select count(p) from Post p where p.moderationStatus = com.eav02.backend.moderation.entity.ModerationStatus.VISIBLE")
    Page<Post> findAllWithUser(Pageable pageable);
}
