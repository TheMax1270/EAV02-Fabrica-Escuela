package com.eav02.backend.interaction.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.eav02.backend.interaction.dto.*;
import com.eav02.backend.interaction.service.CommentService;

@RestController
@Validated
@RequestMapping("/api/v1")
public class CommentController {
    private final CommentService comments;
    public CommentController(CommentService comments) { this.comments = comments; }
    @GetMapping("/projects/{id}/comments")
    public CommentPageResponse project(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return comments.listProject(id, page, size); }
    @GetMapping("/posts/{id}/comments")
    public CommentPageResponse post(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return comments.listPost(id, page, size); }
    @GetMapping("/posts/{id}/comments/count")
    public CommentCountResponse postCount(@PathVariable UUID id) {
        return new CommentCountResponse(comments.countPost(id));
    }
    @GetMapping("/comments/{id}/replies")
    public CommentPageResponse replies(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return comments.listReplies(id, page, size); }
    @PostMapping("/projects/{id}/comments")
    public ResponseEntity<CommentResponse> project(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comments.commentProject(id, UUID.fromString(jwt.getSubject()), request));
    }
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentResponse> post(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comments.commentPost(id, UUID.fromString(jwt.getSubject()), request));
    }
    @PostMapping("/comments/{id}/replies")
    public ResponseEntity<CommentResponse> reply(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comments.reply(id, UUID.fromString(jwt.getSubject()), request));
    }
}
