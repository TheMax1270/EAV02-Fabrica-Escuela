package com.eav02.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.auth.dto.RegistrationResponse;
import com.eav02.backend.auth.dto.LoginRequest;
import com.eav02.backend.auth.dto.LoginResponse;
import com.eav02.backend.auth.dto.RefreshRequest;
import com.eav02.backend.auth.dto.LogoutRequest;
import com.eav02.backend.auth.dto.MeResponse;
import com.eav02.backend.auth.service.IAuthService;
import com.eav02.backend.auth.service.IRegistrationService;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IRegistrationService registrationService;
    private final IAuthService authService;

    public AuthController(IRegistrationService registrationService, IAuthService authService) {
        this.registrationService = registrationService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(UUID.fromString(jwt.getClaimAsString("sid")), UUID.fromString(jwt.getSubject()));
    }
}
