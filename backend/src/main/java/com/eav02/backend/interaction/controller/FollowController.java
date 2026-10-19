package com.eav02.backend.interaction.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.interaction.dto.FollowPageResponse;
import com.eav02.backend.interaction.dto.FollowSummaryResponse;
import com.eav02.backend.interaction.service.FollowService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/users/{userId}")
public class FollowController {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "20";

    private final FollowService follows;

    public FollowController(FollowService follows) {
        this.follows = follows;
    }

    @PutMapping("/followers/me")
    public ResponseEntity<Void> follow(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        follows.follow(viewer(jwt), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/followers/me")
    public ResponseEntity<Void> unfollow(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        follows.unfollow(viewer(jwt), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/follow-summary")
    public ResponseEntity<FollowSummaryResponse> summary(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = jwt == null ? null : viewer(jwt);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(follows.summary(userId, viewerId));
    }

    @GetMapping("/followers")
    public FollowPageResponse followers(@PathVariable UUID userId,
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(100) int size) {
        return follows.followers(userId, page, size);
    }

    @GetMapping("/following")
    public FollowPageResponse following(@PathVariable UUID userId,
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(100) int size) {
        return follows.following(userId, page, size);
    }

    private UUID viewer(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
