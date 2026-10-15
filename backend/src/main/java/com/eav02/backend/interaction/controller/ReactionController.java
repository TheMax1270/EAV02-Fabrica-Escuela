package com.eav02.backend.interaction.controller;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.eav02.backend.interaction.dto.ReactionSummaryResponse;
import com.eav02.backend.interaction.service.ReactionService;

@RestController
@RequestMapping("/api/v1")
public class ReactionController {
    private final ReactionService reactions;
    public ReactionController(ReactionService reactions) { this.reactions = reactions; }
    private UUID user(Jwt jwt) { return jwt == null ? null : UUID.fromString(jwt.getSubject()); }
    @GetMapping("/projects/{id}/likes")
    public ResponseEntity<ReactionSummaryResponse> project(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reactions.summary(id, true, user(jwt)));
    }
    @GetMapping("/posts/{id}/likes")
    public ResponseEntity<ReactionSummaryResponse> post(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reactions.summary(id, false, user(jwt)));
    }
    @PutMapping("/projects/{id}/likes/me")
    public ResponseEntity<Void> likeProject(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        reactions.like(id, true, user(jwt)); return ResponseEntity.noContent().build();
    }
    @PutMapping("/posts/{id}/likes/me")
    public ResponseEntity<Void> likePost(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        reactions.like(id, false, user(jwt)); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/projects/{id}/likes/me")
    public ResponseEntity<Void> unlikeProject(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        reactions.unlike(id, true, user(jwt)); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/posts/{id}/likes/me")
    public ResponseEntity<Void> unlikePost(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        reactions.unlike(id, false, user(jwt)); return ResponseEntity.noContent().build();
    }
}
