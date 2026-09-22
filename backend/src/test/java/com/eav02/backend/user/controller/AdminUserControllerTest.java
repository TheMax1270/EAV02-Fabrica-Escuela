package com.eav02.backend.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.SelfSuspensionException;
import com.eav02.backend.common.exception.UserStatusConflictException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.user.dto.AdminUserResponse;
import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.service.IAdminUserService;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IAdminUserService adminUsers;

    @MockitoBean
    private JwtDecoder decoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void nonAdminRequestsAreRejectedWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListUsers() throws Exception {
        authenticateAsAdmin();
        var response = new AdminUserResponse(UUID.randomUUID(), "Ada Lovelace", "ada", "ada@example.com",
                UserRole.DEVELOPER, AccountStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"));
        when(adminUsers.list(eq(AccountStatus.ACTIVE), eq(0), eq(20)))
                .thenReturn(new com.eav02.backend.user.dto.UserPageResponse(List.of(response), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/admin/users?status=ACTIVE").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("ada"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void suspendingReturnsConflictWhenAlreadySuspended() throws Exception {
        authenticateAsAdmin();
        UUID userId = UUID.randomUUID();
        when(adminUsers.suspend(adminId, userId)).thenThrow(new UserStatusConflictException(AccountStatus.SUSPENDED));

        mvc.perform(post("/api/v1/admin/users/" + userId + "/suspend").header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_STATUS_CONFLICT"));
    }

    @Test
    void adminCannotSuspendThemselves() throws Exception {
        authenticateAsAdmin();
        when(adminUsers.suspend(adminId, adminId)).thenThrow(new SelfSuspensionException());

        mvc.perform(post("/api/v1/admin/users/" + adminId + "/suspend").header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_SUSPENSION_FORBIDDEN"));
    }

    @Test
    void reactivatesASuspendedUser() throws Exception {
        authenticateAsAdmin();
        UUID userId = UUID.randomUUID();
        var response = new AdminUserResponse(userId, "Grace Hopper", "grace", "grace@example.com",
                UserRole.DEVELOPER, AccountStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"));
        when(adminUsers.reactivate(adminId, userId)).thenReturn(response);

        mvc.perform(post("/api/v1/admin/users/" + userId + "/reactivate").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private void authenticateAsAdmin() {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject(adminId.toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY),
                        new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}