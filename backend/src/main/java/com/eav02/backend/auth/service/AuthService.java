package com.eav02.backend.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.auth.dto.LoginRequest;
import com.eav02.backend.auth.dto.LoginResponse;
import com.eav02.backend.auth.dto.MeResponse;
import com.eav02.backend.auth.entity.AuthSession;
import com.eav02.backend.auth.repository.AuthSessionRepository;
import com.eav02.backend.common.exception.AccountDisabledException;
import com.eav02.backend.common.exception.InvalidCredentialsException;
import com.eav02.backend.common.exception.InvalidRefreshTokenException;
import com.eav02.backend.common.validation.BcryptPasswordLengthValidator;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class AuthService implements IAuthService {

    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public AuthService(UserRepository users, AuthSessionRepository sessions,
            PasswordEncoder passwords, TokenService tokens) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim().toLowerCase(Locale.ROOT);
        var matches = users.findByLoginIdentifier(identifier);
        if (matches.size() != 1 || !BcryptPasswordLengthValidator.isWithinLimit(request.password())) {
            throw new InvalidCredentialsException();
        }
        AppUser user = matches.getFirst();
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        String refresh = tokens.newRefreshToken();
        var session = new AuthSession(UUID.randomUUID(), user, tokens.hashRefreshToken(refresh),
                tokens.now(), tokens.now().plus(TokenService.REFRESH_LIFETIME));
        sessions.saveAndFlush(session);
        return response(user, session, refresh);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        AuthSession session = activeSessionForUpdate(refreshToken);
        if (!session.getUser().isEnabled()) {
            throw new InvalidRefreshTokenException();
        }
        String replacement = tokens.newRefreshToken();
        session.rotate(tokens.hashRefreshToken(replacement));
        sessions.flush();
        return response(session.getUser(), session, replacement);
    }

    @Transactional
    public void logout(String refreshToken) {
        AuthSession session = activeSessionForUpdate(refreshToken);
        session.revoke(tokens.now());
        sessions.flush();
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID sessionId, UUID userId) {
        AuthSession session = sessions.findWithUserById(sessionId).orElseThrow(InvalidCredentialsException::new);
        if (!session.isActive(tokens.now()) || !session.getUser().isEnabled()
                || !session.getUser().getId().equals(userId)) {
            throw new InvalidCredentialsException();
        }
        return MeResponse.from(session.getUser());
    }

    private AuthSession activeSessionForUpdate(String refreshToken) {
        String hash = tokens.hashRefreshToken(refreshToken);
        AuthSession session = sessions.findByRefreshTokenHashForUpdate(hash)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!session.isActive(tokens.now())) {
            throw new InvalidRefreshTokenException();
        }
        return session;
    }

    private LoginResponse response(AppUser user, AuthSession session, String refreshToken) {
        return new LoginResponse(tokens.issueAccessToken(user.getId(), session.getId()), refreshToken,
                "Bearer", TokenService.ACCESS_LIFETIME.toSeconds(), MeResponse.from(user));
    }
}
