package com.eav02.backend.interaction.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.interaction.dto.FollowedIdsResponse;
import com.eav02.backend.interaction.service.FollowService;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/v1/me/following")
public class MyFollowingController {

    private final FollowService follows;

    public MyFollowingController(FollowService follows) {
        this.follows = follows;
    }

    @GetMapping
    public ResponseEntity<FollowedIdsResponse> followedAmong(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @NotEmpty @Size(max = 100) List<UUID> userIds) {
        var result = follows.followedAmong(UUID.fromString(jwt.getSubject()), userIds);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
}
