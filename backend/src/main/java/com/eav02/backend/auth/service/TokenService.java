package com.eav02.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.eav02.backend.common.exception.InvalidRefreshTokenException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class TokenService {

    public static final Duration ACCESS_LIFETIME = Duration.ofMinutes(15);
    public static final Duration REFRESH_LIFETIME = Duration.ofDays(7);

    private static final Pattern REFRESH_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(@Value("${jwt.secret}") String encodedSecret,
            @Value("${jwt.issuer}") String issuer, @Value("${jwt.audience}") String audience,
            Clock clock) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("JWT_SECRET debe ser Base64 valido", failure);
        }
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe contener al menos 256 bits aleatorios");
        }
        var key = new SecretKeySpec(secret, "HmacSHA256");
        this.clock = clock;
        this.issuer = issuer;
        this.audience = audience;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var standard = JwtValidators.createDefaultWithIssuer(issuer);
        jwtDecoder.setJwtValidator(jwt -> {
            var result = standard.validate(jwt);
            if (result.hasErrors()) return result;
            if (!jwt.getAudience().contains(audience) || !"access".equals(jwt.getClaimAsString("token_use"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
            try {
                UUID.fromString(jwt.getSubject());
                UUID.fromString(jwt.getClaimAsString("sid"));
            } catch (RuntimeException failure) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
            return OAuth2TokenValidatorResult.success();
        });
        this.decoder = jwtDecoder;
    }

    public Instant now() { return clock.instant(); }

    public JwtDecoder decoder() { return decoder; }

    public String issueAccessToken(UUID userId, UUID sessionId) {
        Instant now = now();
        var claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience(java.util.List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_LIFETIME))
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .claim("token_use", "access")
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String token) {
        if (token == null || !REFRESH_FORMAT.matcher(token).matches()) {
            throw new InvalidRefreshTokenException();
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 no disponible", failure);
        }
    }
}
