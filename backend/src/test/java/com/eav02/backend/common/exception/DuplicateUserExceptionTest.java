package com.eav02.backend.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import com.eav02.backend.user.entity.AccountStatus;

class DuplicateUserExceptionTest {

    @Test
    void exposesTheDuplicatedField() {
        var failure = new DuplicateUserException("email");

        assertEquals("email", failure.getField());
        assertEquals("El email ya esta registrado", failure.getMessage());
    }

    @Test
    void findsTheConstraintDeepInTheCauseChain() {
        var root = new ConstraintViolationException("v", new SQLException("x"), "ux_app_users_username_ci");
        var wrapped = new RuntimeException("externo", new RuntimeException("medio", root));

        var result = DuplicateUserException.fromConstraint(wrapped);

        assertTrue(result.isPresent());
        assertEquals("username", result.get().getField());
    }

    @Test
    void ignoresUnrelatedFailures() {
        var other = new ConstraintViolationException("v", new SQLException("x"), "otra");

        assertTrue(DuplicateUserException.fromConstraint(other).isEmpty());
        assertTrue(DuplicateUserException.fromConstraint(new IllegalStateException()).isEmpty());
    }

    @Test
    void userStatusConflictKeepsTheCurrentStatus() {
        var failure = new UserStatusConflictException(AccountStatus.SUSPENDED);

        assertEquals(AccountStatus.SUSPENDED, failure.getCurrentStatus());
        assertTrue(failure.getMessage().contains("SUSPENDED"));
    }
}
