package com.eav02.backend.interaction.dto;

public record FollowSummaryResponse(long followersCount, long followingCount, Boolean followedByMe) {
}
