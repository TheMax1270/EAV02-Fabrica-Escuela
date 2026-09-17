package com.eav02.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import com.eav02.backend.common.exception.InvalidRefreshTokenException;

class TokenServiceTest {

    private final String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));

    @Test
    void issuesSignedAccessJwtWithExpectedClaims() {
        var tokens = new TokenService(secret, "http://localhost:8080", "eav02-api", Clock.systemUTC());
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();

        var jwt = tokens.decoder().decode(tokens.issueAccessToken(userId, sid));

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(sid.toString(), jwt.getClaimAsString("sid"));
        assertEquals("access", jwt.getClaimAsString("token_use"));
        assertEquals("http://localhost:8080", jwt.getIssuer().toString());
        assertTrue(jwt.getAudience().contains("eav02-api"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertFalse(jwt.getClaims().containsKey("email"));
        assertFalse(jwt.getClaims().containsKey("passwordHash"));
    }

    @Test
    void rejectsExpiredAccessJwt() {
        var oldClock = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        var old = new TokenService(secret, "eav02-backend", "eav02-api", oldClock);
        var current = new TokenService(secret, "eav02-backend", "eav02-api", Clock.systemUTC());
        String jwt = old.issueAccessToken(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(JwtException.class, () -> current.decoder().decode(jwt));
    }

    @Test
    void rejectsWrongAudienceAndSignature() {
        var tokens = new TokenService(secret, "eav02-backend", "eav02-api", Clock.systemUTC());
        var otherAudience = new TokenService(secret, "eav02-backend", "other-api", Clock.systemUTC());
        var otherKey = new TokenService(Base64.getEncoder().encodeToString(
                "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.US_ASCII)),
                "eav02-backend", "eav02-api", Clock.systemUTC());
        String jwt = tokens.issueAccessToken(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(JwtException.class, () -> otherAudience.decoder().decode(jwt));
        assertThrows(JwtException.class, () -> otherKey.decoder().decode(jwt));
    }

    @Test
    void refreshTokensAreRandomAndOnlyHashIsPersistable() {
        var tokens = new TokenService(secret, "eav02-backend", "eav02-api", Clock.systemUTC());
        String first = tokens.newRefreshToken();
        String second = tokens.newRefreshToken();
        assertEquals(43, first.length());
        assertNotEquals(first, second);
        assertEquals(64, tokens.hashRefreshToken(first).length());
        assertNotEquals(first, tokens.hashRefreshToken(first));
        assertThrows(InvalidRefreshTokenException.class, () -> tokens.hashRefreshToken("bad"));
    }

    @Test
    void refusesMissingOrShortSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenService("", "issuer", "audience", Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,
                () -> new TokenService("not base64!", "issuer", "audience", Clock.systemUTC()));
    }
}
