package com.eav02.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.auth.dto.LoginRequest;
import com.eav02.backend.auth.entity.AuthSession;
import com.eav02.backend.auth.repository.AuthSessionRepository;
import com.eav02.backend.common.exception.AccountDisabledException;
import com.eav02.backend.common.exception.InvalidCredentialsException;
import com.eav02.backend.common.exception.InvalidRefreshTokenException;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock UserRepository users;
    @Mock AuthSessionRepository sessions;
    @Mock PasswordEncoder passwords;
    @Mock TokenService tokens;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, sessions, passwords, tokens);
    }

    @Test
    void loginNormalizesIdentifierAndCreatesSession() {
        AppUser user = user(true);
        when(users.findByLoginIdentifier("ada@example.com")).thenReturn(List.of(user));
        when(passwords.matches("secret123", user.getPasswordHash())).thenReturn(true);
        when(tokens.newRefreshToken()).thenReturn("refresh");
        when(tokens.hashRefreshToken("refresh")).thenReturn("hash");
        when(tokens.now()).thenReturn(NOW);
        when(tokens.issueAccessToken(any(), any())).thenReturn("access");

        var result = service.login(new LoginRequest(" ADA@Example.com ", "secret123"));

        ArgumentCaptor<AuthSession> saved = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).saveAndFlush(saved.capture());
        assertEquals(user.getId(), saved.getValue().getUser().getId());
        assertEquals("hash", saved.getValue().getRefreshTokenHash());
        assertEquals(NOW.plus(TokenService.REFRESH_LIFETIME), saved.getValue().getExpiresAt());
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900, result.expiresIn());
        assertEquals("ada", result.user().username());
    }

    @Test
    void wrongPasswordIsGenericUnauthorized() {
        AppUser user = user(true);
        when(users.findByLoginIdentifier("ada")).thenReturn(List.of(user));
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("ada", "wrong123")));
        verifyNoInteractions(sessions, tokens);
    }

    @Test
    void unknownUserIsGenericUnauthorized() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("unknown", "secret123")));
        verifyNoInteractions(passwords, sessions, tokens);
    }

    @Test
    void ambiguousIdentifierDoesNotTryEitherPassword() {
        when(users.findByLoginIdentifier("same")).thenReturn(List.of(user(true), user(true)));
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest(" SAME ", "secret123")));
        verifyNoInteractions(passwords, sessions, tokens);
    }

    @Test
    void disabledAccountIsOnlyRevealedAfterCorrectPassword() {
        AppUser user = user(false);
        when(users.findByLoginIdentifier("ada")).thenReturn(List.of(user));
        when(passwords.matches("secret123", user.getPasswordHash())).thenReturn(true);
        assertThrows(AccountDisabledException.class,
                () -> service.login(new LoginRequest("ada", "secret123")));
        verifyNoInteractions(sessions, tokens);
    }

    @Test
    void refreshRotatesHashAndIssuesNewTokens() {
        AuthSession session = session(NOW.plusSeconds(3600));
        when(tokens.hashRefreshToken("old")).thenReturn("old-hash");
        when(tokens.hashRefreshToken("new")).thenReturn("new-hash");
        when(tokens.now()).thenReturn(NOW);
        when(tokens.newRefreshToken()).thenReturn("new");
        when(tokens.issueAccessToken(any(), any())).thenReturn("new-access");
        when(sessions.findByRefreshTokenHashForUpdate("old-hash")).thenReturn(Optional.of(session));

        var response = service.refresh("old");

        assertEquals("new", response.refreshToken());
        assertEquals("new-hash", session.getRefreshTokenHash());
        assertNotEquals("old-hash", session.getRefreshTokenHash());
        verify(sessions).flush();
    }

    @Test
    void expiredRefreshIsRejected() {
        AuthSession session = session(NOW.minusSeconds(1));
        when(tokens.hashRefreshToken("old")).thenReturn("old-hash");
        when(tokens.now()).thenReturn(NOW);
        when(sessions.findByRefreshTokenHashForUpdate("old-hash")).thenReturn(Optional.of(session));
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("old"));
        verify(sessions, never()).flush();
    }

    @Test
    void revokedRefreshIsRejected() {
        AuthSession session = session(NOW.plusSeconds(3600));
        session.revoke(NOW.minusSeconds(1));
        when(tokens.hashRefreshToken("old")).thenReturn("old-hash");
        when(tokens.now()).thenReturn(NOW);
        when(sessions.findByRefreshTokenHashForUpdate("old-hash")).thenReturn(Optional.of(session));
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("old"));
    }

    @Test
    void disabledUserCannotRefresh() {
        AuthSession session = new AuthSession(UUID.randomUUID(), user(false), "old-hash",
                NOW.minusSeconds(10), NOW.plusSeconds(3600));
        when(tokens.hashRefreshToken("old")).thenReturn("old-hash");
        when(tokens.now()).thenReturn(NOW);
        when(sessions.findByRefreshTokenHashForUpdate("old-hash")).thenReturn(Optional.of(session));
        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh("old"));
    }

    @Test
    void logoutRevokesSessionAndMeRejectsOldAccess() {
        AuthSession session = session(NOW.plusSeconds(3600));
        when(tokens.hashRefreshToken("old")).thenReturn("old-hash");
        when(tokens.now()).thenReturn(NOW);
        when(sessions.findByRefreshTokenHashForUpdate("old-hash")).thenReturn(Optional.of(session));
        when(sessions.findWithUserById(session.getId())).thenReturn(Optional.of(session));

        service.logout("old");

        assertNotNull(session.getRevokedAt());
        verify(sessions).flush();
        assertThrows(InvalidCredentialsException.class,
                () -> service.me(session.getId(), session.getUser().getId()));
    }

    private AppUser user(boolean enabled) {
        var user = new AppUser("Ada Lovelace", "ada", "ada@example.com", "encoded",
                UserRole.DEVELOPER, enabled, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private AuthSession session(Instant expiresAt) {
        return new AuthSession(UUID.randomUUID(), user(true), "old-hash", NOW.minusSeconds(10), expiresAt);
    }
}
