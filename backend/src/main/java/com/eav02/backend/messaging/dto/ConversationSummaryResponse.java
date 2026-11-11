package com.eav02.backend.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(UUID conversationId, ParticipantResponse participant,
        MessageResponse lastMessage, Instant lastMessageAt) {
}
