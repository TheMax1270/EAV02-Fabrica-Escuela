package com.eav02.backend.common.exception;

public class PasswordTooLongException extends RuntimeException {

    public PasswordTooLongException() {
        super("La contrasena no debe superar 72 bytes UTF-8");
    }
}
