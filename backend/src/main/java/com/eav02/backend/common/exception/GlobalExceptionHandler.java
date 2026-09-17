package com.eav02.backend.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException failure) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("INVALID_CREDENTIALS",
                "Credenciales invalidas", Map.of()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiError> invalidRefreshToken(InvalidRefreshTokenException failure) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("INVALID_TOKEN",
                "Token invalido", Map.of()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    ResponseEntity<ApiError> accountDisabled(AccountDisabledException failure) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("ACCOUNT_DISABLED",
                "La cuenta no esta habilitada", Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidJson(HttpMessageNotReadableException failure) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_JSON", "Solicitud JSON invalida", Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException failure) {
        Map<String, String> errors = new LinkedHashMap<>();
        failure.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Datos invalidos", errors));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    ResponseEntity<ApiError> passwordMismatch(PasswordMismatchException failure) {
        return ResponseEntity.badRequest().body(new ApiError("PASSWORD_MISMATCH", failure.getMessage(),
                Map.of("passwordConfirmation", failure.getMessage())));
    }

    @ExceptionHandler(PasswordTooLongException.class)
    ResponseEntity<ApiError> passwordTooLong(PasswordTooLongException failure) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Datos invalidos",
                Map.of("password", failure.getMessage())));
    }

    @ExceptionHandler(DuplicateUserException.class)
    ResponseEntity<ApiError> duplicate(DuplicateUserException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("DUPLICATE_USER",
                failure.getMessage(), Map.of(failure.getField(), failure.getMessage())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException failure) {
        return DuplicateUserException.fromConstraint(failure)
                .map(this::duplicate)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiError("INTERNAL_ERROR", "No se pudo completar la operacion", Map.of())));
    }
}
