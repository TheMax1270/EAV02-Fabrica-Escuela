package com.eav02.backend.messaging.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record ConversationPageResponse(List<ConversationSummaryResponse> content, int page, int size,
        long totalElements, int totalPages) {

    public static ConversationPageResponse from(Page<ConversationSummaryResponse> result) {
        return new ConversationPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
