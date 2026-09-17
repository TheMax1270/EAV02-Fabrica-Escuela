package com.eav02.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.auth.entity.AuthSession;
import com.eav02.backend.auth.repository.AuthSessionRepository;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class SessionJwtAuthenticationConverterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    @Mock AuthSessionRepository sessions;

    @Test
    void usesCurrentDatabaseRole() {
        var session = session(true);
        var jwt = jwt(session);
        when(sessions.findWithUserById(session.getId())).thenReturn(Optional.of(session));
        var converter = new SessionJwtAuthenticationConverter(sessions, Clock.fixed(NOW, ZoneOffset.UTC));

        var authentication = converter.convert(jwt);

        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void rejectsRevokedSessionImmediately() {
        var session = session(true);
        session.revoke(NOW);
        when(sessions.findWithUserById(session.getId())).thenReturn(Optional.of(session));
        var converter = new SessionJwtAuthenticationConverter(sessions, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt(session)));
    }

    @Test
    void rejectsSuspendedUserImmediately() {
        var session = session(false);
        when(sessions.findWithUserById(session.getId())).thenReturn(Optional.of(session));
        var converter = new SessionJwtAuthenticationConverter(sessions, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt(session)));
    }

    private AuthSession session(boolean enabled) {
        var user = new AppUser("Ada", "ada", "ada@example.com", "encoded", UserRole.ADMIN, enabled, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return new AuthSession(UUID.randomUUID(), user, "hash", NOW.minusSeconds(5), NOW.plusSeconds(100));
    }

    private Jwt jwt(AuthSession session) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .subject(session.getUser().getId().toString())
                .claim("sid", session.getId().toString()).build();
    }
}
