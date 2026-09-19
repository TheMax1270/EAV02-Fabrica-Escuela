package com.eav02.backend.user.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.eav02.backend.user.dto.AdminUserResponse;
import com.eav02.backend.user.dto.UserPageResponse;
import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.service.AdminUserService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUsers;

    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = adminUsers;
    }

    @GetMapping
    public UserPageResponse list(@RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminUsers.list(status, page, size);
    }

    @PostMapping("/{userId}/suspend")
    public AdminUserResponse suspend(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        return adminUsers.suspend(adminId(jwt), userId);
    }

    @PostMapping("/{userId}/reactivate")
    public AdminUserResponse reactivate(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        return adminUsers.reactivate(adminId(jwt), userId);
    }

    private UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
