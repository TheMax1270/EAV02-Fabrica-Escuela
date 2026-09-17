package com.eav02.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.common.exception.DuplicateUserException;
import com.eav02.backend.common.exception.PasswordMismatchException;
import com.eav02.backend.common.exception.PasswordTooLongException;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository users;

    private BCryptPasswordEncoder encoder;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new RegistrationService(users, encoder);
    }

    @Test
    void registersDeveloperWithNormalizedDataAndEncodedPassword() {
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        var response = service.register(new RegistrationRequest(" Ada Lovelace ", " Ada ",
                " ADA@Example.com ", "secret123", "secret123"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("Ada Lovelace", saved.getFullName());
        assertEquals("ada", saved.getUsername());
        assertEquals("ada@example.com", saved.getEmail());
        assertTrue(encoder.matches("secret123", saved.getPasswordHash()));
        assertFalse("secret123".equals(saved.getPasswordHash()));
        assertEquals(UserRole.DEVELOPER, response.role());
        assertTrue(response.enabled());
        assertEquals(saved.getId(), response.id());
    }

    @Test
    void rejectsPasswordMismatchBeforeWriting() {
        var request = new RegistrationRequest("Ada Lovelace", "ada", "ada@example.com",
                "secret123", "different123");

        assertThrows(PasswordMismatchException.class, () -> service.register(request));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmailNormalized("ada@example.com")).thenReturn(true);

        DuplicateUserException error = assertThrows(DuplicateUserException.class,
                () -> service.register(validRequest()));

        assertEquals("email", error.getField());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateUsername() {
        when(users.existsByUsernameNormalized("ada")).thenReturn(true);

        DuplicateUserException error = assertThrows(DuplicateUserException.class,
                () -> service.register(validRequest()));

        assertEquals("username", error.getField());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void mapsConcurrentUniqueIndexCollisionToConflict() {
        var constraint = new ConstraintViolationException("duplicate", new SQLException(),
                "ux_app_users_email_ci");
        when(users.saveAndFlush(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", constraint));

        DuplicateUserException error = assertThrows(DuplicateUserException.class,
                () -> service.register(validRequest()));

        assertEquals("email", error.getField());
    }

    @Test
    void mapsConcurrentUsernameCollisionToConflict() {
        var constraint = new ConstraintViolationException("duplicate", new SQLException(),
                "ux_app_users_username_ci");
        when(users.saveAndFlush(any(AppUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", constraint));

        DuplicateUserException error = assertThrows(DuplicateUserException.class,
                () -> service.register(validRequest()));

        assertEquals("username", error.getField());
    }

    @Test
    void rejectsOversizedPasswordBeforeCallingEncoderOrRepository() {
        PasswordEncoder guardedEncoder = mock(PasswordEncoder.class);
        service = new RegistrationService(users, guardedEncoder);
        String password = "a".repeat(72) + "1";
        var request = new RegistrationRequest("Ada Lovelace", "ada", "ada@example.com",
                password, password);

        assertThrows(PasswordTooLongException.class, () -> service.register(request));
        verifyNoInteractions(guardedEncoder, users);
    }

    private RegistrationRequest validRequest() {
        return new RegistrationRequest("Ada Lovelace", "ada", "ada@example.com",
                "secret123", "secret123");
    }
}
