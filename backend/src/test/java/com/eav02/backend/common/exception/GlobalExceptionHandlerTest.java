package com.eav02.backend.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static DataIntegrityViolationException integrity(String constraint) {
        var cause = new ConstraintViolationException("violacion", new SQLException("x"), constraint);
        return new DataIntegrityViolationException("error", cause);
    }

    @Test
    void duplicatedEmailConstraintBecomesConflict() {
        var response = handler.dataIntegrity(integrity("ux_app_users_email_ci"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DUPLICATE_USER", response.getBody().code());
        assertTrue(response.getBody().errors().containsKey("email"));
    }

    @Test
    void duplicatedUsernameConstraintBecomesConflict() {
        var response = handler.dataIntegrity(integrity("ux_app_users_username_ci"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().errors().containsKey("username"));
    }

    @Test
    void unknownIntegrityViolationBecomesInternalError() {
        var response = handler.dataIntegrity(integrity("otra_restriccion"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
    }

    @Test
    void integrityViolationWithoutConstraintCauseBecomesInternalError() {
        var response = handler.dataIntegrity(new DataIntegrityViolationException("sin causa"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
