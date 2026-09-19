package com.eav02.backend.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

public record AdminUserResponse(UUID id, String fullName, String username, String email, UserRole role,
        AccountStatus status, Instant createdAt) {

    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
