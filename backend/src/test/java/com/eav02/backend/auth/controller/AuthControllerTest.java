package com.eav02.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.auth.dto.RegistrationResponse;
import com.eav02.backend.auth.dto.LoginResponse;
import com.eav02.backend.auth.dto.MeResponse;
import com.eav02.backend.auth.dto.LoginRequest;
import com.eav02.backend.auth.service.AuthService;
import com.eav02.backend.auth.service.RegistrationService;
import com.eav02.backend.common.exception.AccountDisabledException;
import com.eav02.backend.common.exception.DuplicateUserException;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.InvalidCredentialsException;
import com.eav02.backend.common.exception.InvalidRefreshTokenException;
import com.eav02.backend.common.exception.PasswordMismatchException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.user.entity.UserRole;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegistrationService service;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder decoder;

    @MockitoBean
    private SessionJwtAuthenticationConverter sessionConverter;

    @Test
    void registersWithoutAuthenticationOrCsrfToken() throws Exception {
        var response = new RegistrationResponse(UUID.randomUUID(), "Ada Lovelace", "ada",
                "ada@example.com", UserRole.DEVELOPER, true, Instant.parse("2026-01-01T00:00:00Z"));
        when(service.register(any(RegistrationRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("ada@example.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        verifyNoInteractions(service);
    }

    @Test
    void trimsEmailAndUsernameBeforeValidation() throws Exception {
        var response = new RegistrationResponse(UUID.randomUUID(), "Ada Lovelace", "ada",
                "ada@example.com", UserRole.DEVELOPER, true, Instant.parse("2026-01-01T00:00:00Z"));
        when(service.register(any(RegistrationRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("ada@example.com", " ada@example.com ")
                        .replace("\"username\":\"ada\"", "\"username\":\" ada \"")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("secret123", "abc1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsPasswordWithoutNumber() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("secret123", "secrettt")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Debe contener al menos un numero"));
        verifyNoInteractions(service);
    }

    @Test
    void acceptsPasswordAtBcryptByteLimit() throws Exception {
        String password = "a".repeat(71) + "1";
        var response = new RegistrationResponse(UUID.randomUUID(), "Ada Lovelace", "ada",
                "ada@example.com", UserRole.DEVELOPER, true, Instant.parse("2026-01-01T00:00:00Z"));
        when(service.register(any(RegistrationRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("secret123", password)))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsPasswordOverBcryptByteLimit() throws Exception {
        String password = "a".repeat(72) + "1";

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("secret123", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password")
                        .value("La contrasena no debe superar 72 bytes UTF-8"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMultibytePasswordOverBcryptByteLimit() throws Exception {
        String password = "é".repeat(36) + "1";

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("secret123", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingRequiredField() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson().replace("\"fullName\":\"Ada Lovelace\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
        verifyNoInteractions(service);
    }

    @Test
    void returnsPasswordMismatchWithoutSensitiveData() throws Exception {
        when(service.register(any(RegistrationRequest.class))).thenThrow(new PasswordMismatchException());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.errors.passwordConfirmation").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(service.register(any(RegistrationRequest.class))).thenThrow(new DuplicateUserException("email"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USER"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void returnsConflictForDatabaseUniqueViolation() throws Exception {
        var constraint = new ConstraintViolationException("duplicate", new SQLException(),
                "ux_app_users_username_ci");
        when(service.register(any(RegistrationRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", constraint));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void rejectsMalformedJsonWithoutInternalDetails() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
        verifyNoInteractions(service);
    }

    @Test
    void deniesOtherEndpoints() throws Exception {
        mvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logsInAndReturnsOnlyPublicUserData() throws Exception {
        var user = new MeResponse(UUID.randomUUID(), "ada", UserRole.DEVELOPER, true);
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("access", "refresh", "Bearer", 900, user));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"ada\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.username").value("ada"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void wrongCredentialsHaveGenericResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"ada\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void disabledAccountReturnsForbidden() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new AccountDisabledException());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"ada\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject(userId.toString()).claim("sid", sid.toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt))
                .thenReturn(new JwtAuthenticationToken(jwt,
                        java.util.List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
        when(authService.me(sid, userId)).thenReturn(new MeResponse(userId, "ada", UserRole.DEVELOPER, true));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"));
    }

    @Test
    void revokedSessionBearerIsUnauthorized() throws Exception {
        Jwt jwt = Jwt.withTokenValue("revoked").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("revoked")).thenReturn(jwt);
        when(sessionConverter.convert(jwt))
                .thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer revoked"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshAndLogoutExposeExpectedStatuses() throws Exception {
        var user = new MeResponse(UUID.randomUUID(), "ada", UserRole.DEVELOPER, true);
        when(authService.refresh("old")).thenReturn(new LoginResponse("new-access", "new-refresh", "Bearer", 900, user));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"new-refresh\"}"))
                .andExpect(status().isNoContent());
        verify(authService).logout("new-refresh");
    }

    @Test
    void rejectedRefreshReturnsUnauthorized() throws Exception {
        when(authService.refresh("old")).thenThrow(new InvalidRefreshTokenException());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    private String validJson() {
        return """
                {"fullName":"Ada Lovelace","username":"ada","email":"ada@example.com",
                 "password":"secret123","passwordConfirmation":"secret123"}
                """;
    }
}
