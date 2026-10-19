package com.eav02.backend.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.config.PasswordConfig;

class SecurityBeansTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void clockIsUtc() {
        assertEquals(ZoneOffset.UTC, config.clock().getZone());
    }

    @Test
    void jwtDecoderComesFromTokenService() {
        var tokens = mock(TokenService.class);
        var decoder = mock(JwtDecoder.class);
        when(tokens.decoder()).thenReturn(decoder);

        assertEquals(decoder, config.jwtDecoder(tokens));
        verify(tokens).decoder();
    }

    @Test
    void corsAllowsOnlyKnownOriginsMethodsAndHeaders() {
        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/posts"));

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(cors.getAllowedOrigins().contains("https://eav-02-fabrica-escuela.vercel.app"));
        assertTrue(cors.getAllowedMethods().containsAll(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
    }

    @Test
    void passwordEncoderHashesAndMatchesWithBcrypt() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        String hash = encoder.encode("Clave1234");

        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("Clave1234", hash));
        assertTrue(!encoder.matches("otra", hash));
    }
}
