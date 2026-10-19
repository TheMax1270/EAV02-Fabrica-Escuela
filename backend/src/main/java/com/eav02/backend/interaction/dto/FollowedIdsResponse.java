package com.eav02.backend.interaction.dto;

import java.util.List;
import java.util.UUID;

public record FollowedIdsResponse(List<UUID> userIds) {
}
