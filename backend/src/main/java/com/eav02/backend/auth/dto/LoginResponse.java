package com.eav02.backend.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType,
        long expiresIn, MeResponse user) {
}
