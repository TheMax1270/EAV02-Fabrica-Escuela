package com.eav02.backend.auth.dto;

import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

public record MeResponse(UUID id, String username, UserRole role, boolean enabled) {
    public static MeResponse from(AppUser user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled());
    }
}
