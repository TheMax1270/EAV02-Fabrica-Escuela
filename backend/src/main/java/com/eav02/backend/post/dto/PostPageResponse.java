package com.eav02.backend.post.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.eav02.backend.post.entity.Post;

public record PostPageResponse(List<PostResponse> content, int page, int size, long totalElements,
        int totalPages) {

    public static PostPageResponse from(Page<Post> posts) {
        return new PostPageResponse(posts.getContent().stream().map(PostResponse::from).toList(),
                posts.getNumber(), posts.getSize(), posts.getTotalElements(), posts.getTotalPages());
    }
}
