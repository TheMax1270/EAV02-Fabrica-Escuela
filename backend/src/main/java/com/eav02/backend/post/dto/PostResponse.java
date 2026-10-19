package com.eav02.backend.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.entity.PostType;

public record PostResponse(UUID id, UUID userId, String username, String title, String content,
        Instant createdAt, PostType type, String link, List<String> categories, boolean resolved) {

    public PostResponse(UUID id, UUID userId, String username, String title, String content, Instant createdAt) {
        this(id, userId, username, title, content, createdAt, PostType.GENERAL, null, List.of(), false);
    }

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getUser().getId(), post.getUser().getUsername(),
                post.getTitle(), post.getContent(), post.getCreatedAt(), post.getType(), post.getLink(),
                post.getCategories(), post.isResolved());
    }
}
