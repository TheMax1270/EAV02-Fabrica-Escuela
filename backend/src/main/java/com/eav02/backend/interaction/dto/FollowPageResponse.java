package com.eav02.backend.interaction.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record FollowPageResponse(List<FollowUserResponse> content, int page, int size, long totalElements,
        int totalPages) {

    public static FollowPageResponse from(Page<FollowUserResponse> result) {
        return new FollowPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
