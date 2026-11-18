package com.eav02.backend.messaging.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.messaging.entity.Message;

public record MessageResponse(UUID id, UUID conversationId, UUID senderId, UUID recipientId, String content,
        Instant createdAt) {

    public static MessageResponse from(Message message, UUID recipientId) {
        return new MessageResponse(message.getId(), message.getConversation().getId(), message.getSender().getId(),
                recipientId, message.getContent(), message.getCreatedAt());
    }
}
