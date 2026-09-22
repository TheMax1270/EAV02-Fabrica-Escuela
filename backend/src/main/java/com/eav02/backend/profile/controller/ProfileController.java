package com.eav02.backend.profile.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.profile.dto.ProfileRequest;
import com.eav02.backend.profile.dto.ProfileResponse;
import com.eav02.backend.profile.service.IProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final IProfileService profiles;

    public ProfileController(IProfileService profiles) {
        this.profiles = profiles;
    }

    @PostMapping("/profile/me")
    public ResponseEntity<ProfileResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profiles.create(userId(jwt), request));
    }

    @GetMapping("/profile/me")
    public ProfileResponse own(@AuthenticationPrincipal Jwt jwt) {
        return profiles.getOwn(userId(jwt));
    }

    @PutMapping("/profile/me")
    public ProfileResponse updateOwn(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileRequest request) {
        return profiles.updateOwn(userId(jwt), request);
    }

    @GetMapping("/profiles/{profileId}")
    public ProfileResponse publicProfile(@PathVariable UUID profileId) {
        return profiles.getPublic(profileId);
    }

    @PutMapping("/profiles/{profileId}")
    public ProfileResponse update(@PathVariable UUID profileId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileRequest request) {
        return profiles.update(profileId, userId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
