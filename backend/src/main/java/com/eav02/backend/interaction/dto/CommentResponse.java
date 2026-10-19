package com.eav02.backend.interaction.dto;
import java.time.Instant;
import java.util.UUID;
import com.eav02.backend.interaction.entity.Comment;
public record CommentResponse(UUID id, UUID userId, String username, UUID projectId, UUID postId,
        UUID parentCommentId, String content, Instant createdAt, long replyCount) {
    public static CommentResponse from(Comment comment, long replyCount) {
        return new CommentResponse(comment.getId(), comment.getUser().getId(), comment.getUser().getUsername(),
                comment.getProject() == null ? null : comment.getProject().getId(),
                comment.getPost() == null ? null : comment.getPost().getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getContent(), comment.getCreatedAt(), replyCount);
    }
}
