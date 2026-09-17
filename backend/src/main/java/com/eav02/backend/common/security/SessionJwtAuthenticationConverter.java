package com.eav02.backend.common.security;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.eav02.backend.auth.repository.AuthSessionRepository;

@Component
public class SessionJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthSessionRepository sessions;
    private final Clock clock;

    public SessionJwtAuthenticationConverter(AuthSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            UUID userId = UUID.fromString(jwt.getSubject());
            var session = sessions.findWithUserById(sessionId).orElseThrow(this::invalidToken);
            var user = session.getUser();
            if (!session.isActive(clock.instant()) || !user.isEnabled() || !user.getId().equals(userId)) {
                throw invalidToken();
            }
            return new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()),
                            new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY)), userId.toString());
        } catch (IllegalArgumentException failure) {
            throw invalidToken();
        }
    }

    private OAuth2AuthenticationException invalidToken() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
    }
}
