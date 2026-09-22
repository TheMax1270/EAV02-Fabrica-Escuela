package com.eav02.backend.auth.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.auth.dto.RegistrationResponse;
import com.eav02.backend.common.exception.DuplicateUserException;
import com.eav02.backend.common.exception.PasswordMismatchException;
import com.eav02.backend.common.exception.PasswordTooLongException;
import com.eav02.backend.common.validation.BcryptPasswordLengthValidator;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class RegistrationService implements IRegistrationService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim().toLowerCase(Locale.ROOT);

        if (!BcryptPasswordLengthValidator.isWithinLimit(request.password())) {
            throw new PasswordTooLongException();
        }
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new PasswordMismatchException();
        }
        if (users.existsByEmailNormalized(email)) {
            throw new DuplicateUserException("email");
        }
        if (users.existsByUsernameNormalized(username)) {
            throw new DuplicateUserException("username");
        }

        AppUser user = new AppUser(request.fullName().trim(), username, email,
                passwordEncoder.encode(request.password()), UserRole.DEVELOPER, true, Instant.now());
        try {
            AppUser saved = users.saveAndFlush(user);
            return new RegistrationResponse(saved.getId(), saved.getFullName(), saved.getUsername(),
                    saved.getEmail(), saved.getRole(), saved.isEnabled(), saved.getCreatedAt());
        } catch (DataIntegrityViolationException failure) {
            var duplicate = DuplicateUserException.fromConstraint(failure);
            if (duplicate.isPresent()) {
                throw duplicate.get();
            }
            throw failure;
        }
    }
}
