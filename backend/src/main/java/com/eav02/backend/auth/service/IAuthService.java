package com.eav02.backend.auth.service;

import java.util.UUID;

import com.eav02.backend.auth.dto.LoginRequest;
import com.eav02.backend.auth.dto.LoginResponse;
import com.eav02.backend.auth.dto.MeResponse;

public interface IAuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(String refreshToken);

    void logout(String refreshToken);

    MeResponse me(UUID sessionId, UUID userId);
}