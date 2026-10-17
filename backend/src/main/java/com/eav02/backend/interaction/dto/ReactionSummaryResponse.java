package com.eav02.backend.interaction.dto;

import java.util.List;

public record ReactionSummaryResponse(long likeCount, Boolean likedByMe, List<String> usernames) {
    public ReactionSummaryResponse(long likeCount, Boolean likedByMe) {
        this(likeCount, likedByMe, List.of());
    }
}
