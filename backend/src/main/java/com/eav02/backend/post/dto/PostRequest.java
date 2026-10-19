package com.eav02.backend.post.dto;

import java.util.List;

import com.eav02.backend.post.entity.PostType;
import com.eav02.backend.post.validation.ValidPost;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ValidPost
public record PostRequest(
        @Size(max = 150) String title,
        @NotBlank @Size(min = 1, max = 5000) String content,
        PostType type,
        @Size(max = 500) String link,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> categories) {

    public PostRequest {
        title = title == null || title.isBlank() ? null : title.trim();
        content = content == null ? null : content.trim();
        type = type == null ? PostType.GENERAL : type;
        link = link == null || link.isBlank() ? null : link.trim();
        categories = categories == null ? List.of()
                : categories.stream().map(value -> value == null ? null : value.trim()).toList();
    }

    public PostRequest(String title, String content) {
        this(title, content, PostType.GENERAL, null, List.of());
    }
}
