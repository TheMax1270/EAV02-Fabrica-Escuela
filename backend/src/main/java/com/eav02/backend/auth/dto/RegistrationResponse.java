package com.eav02.backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.user.entity.UserRole;

public record RegistrationResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt) {
}
