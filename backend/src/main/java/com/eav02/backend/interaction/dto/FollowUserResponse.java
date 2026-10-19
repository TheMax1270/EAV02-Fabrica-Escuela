package com.eav02.backend.interaction.dto;

import java.time.Instant;
import java.util.UUID;

public record FollowUserResponse(UUID userId, String username, Instant followedAt) {
}
