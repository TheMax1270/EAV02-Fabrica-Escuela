package com.eav02.backend.messaging.dto;

import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;

public record ParticipantResponse(UUID id, String username) {

    public static ParticipantResponse from(AppUser user) {
        return new ParticipantResponse(user.getId(), user.getUsername());
    }
}
