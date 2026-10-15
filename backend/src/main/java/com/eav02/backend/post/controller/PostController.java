package com.eav02.backend.post.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.post.dto.PostPageResponse;
import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.dto.PostResponse;
import com.eav02.backend.post.service.PostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        posts.delete(postId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(posts.create(UUID.fromString(jwt.getSubject()), request));
    }

    @GetMapping
    public PostPageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return posts.list(page, size);
    }

    @GetMapping("/{postId}")
    public PostResponse getById(@PathVariable UUID postId) {
        return posts.getById(postId);
    }

    @PatchMapping("/{postId}/resolve")
    public PostResponse resolve(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        return posts.resolve(postId, UUID.fromString(jwt.getSubject()));
    }
}
