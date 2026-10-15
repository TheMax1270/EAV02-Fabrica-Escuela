package com.eav02.backend.interaction.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CommentRequest(@NotBlank @Size(max = 2000) String content) {
    public CommentRequest { content = content == null ? null : content.trim(); }
}
