package com.eav02.backend.interaction.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.interaction.dto.BlockSummaryResponse;
import com.eav02.backend.interaction.service.BlockService;

@RestController
@RequestMapping("/api/v1/users/{userId}")
public class BlockController {

    private final BlockService blocks;

    public BlockController(BlockService blocks) {
        this.blocks = blocks;
    }

    @PutMapping("/blocks/me")
    public ResponseEntity<Void> block(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        blocks.block(viewer(jwt), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/blocks/me")
    public ResponseEntity<Void> unblock(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        blocks.unblock(viewer(jwt), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/block-summary")
    public ResponseEntity<BlockSummaryResponse> summary(@PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = jwt == null ? null : viewer(jwt);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(blocks.summary(userId, viewerId));
    }

    private UUID viewer(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
