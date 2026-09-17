package com.eav02.backend.common.exception;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

public class DuplicateUserException extends RuntimeException {

    private final String field;

    public DuplicateUserException(String field) {
        super("El " + field + " ya esta registrado");
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public static Optional<DuplicateUserException> fromConstraint(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if ("ux_app_users_email_ci".equals(name)) {
                    return Optional.of(new DuplicateUserException("email"));
                }
                if ("ux_app_users_username_ci".equals(name)) {
                    return Optional.of(new DuplicateUserException("username"));
                }
            }
        }
        return Optional.empty();
    }
}
