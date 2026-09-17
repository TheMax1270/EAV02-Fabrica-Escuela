package com.eav02.backend.common.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Las contrasenas no coinciden");
    }
}
