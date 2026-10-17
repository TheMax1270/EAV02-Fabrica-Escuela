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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommentNotFoundException.class)
    ResponseEntity<ApiError> commentNotFound(CommentNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("COMMENT_NOT_FOUND",
                "Comentario no encontrado", Map.of()));
    }

    @ExceptionHandler(ReplyDepthException.class)
    ResponseEntity<ApiError> replyDepth(ReplyDepthException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("REPLY_DEPTH_EXCEEDED",
                "Solo puedes responder a un comentario raiz", Map.of()));
    }

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

    @ExceptionHandler(ReportNotFoundException.class)
    ResponseEntity<ApiError> reportNotFound(ReportNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("REPORT_NOT_FOUND",
                "Reporte no encontrado", Map.of()));
    }

    @ExceptionHandler(ModerationTargetNotFoundException.class)
    ResponseEntity<ApiError> moderationTargetNotFound(ModerationTargetNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("TARGET_NOT_FOUND",
                "El elemento reportado no existe", Map.of()));
    }

    @ExceptionHandler(ModerationStateConflictException.class)
    ResponseEntity<ApiError> moderationStateConflict(ModerationStateConflictException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("MODERATION_STATE_CONFLICT",
                "El reporte ya fue procesado", Map.of()));
    }

    @ExceptionHandler(ProfileAlreadyExistsException.class)
    ResponseEntity<ApiError> profileAlreadyExists(ProfileAlreadyExistsException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("PROFILE_ALREADY_EXISTS",
                "El usuario ya tiene un perfil tecnico", Map.of()));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<ApiError> profileNotFound(ProfileNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("PROFILE_NOT_FOUND",
                "Perfil tecnico no encontrado", Map.of()));
    }

    @ExceptionHandler(ProfileOwnershipException.class)
    ResponseEntity<ApiError> profileOwnership(ProfileOwnershipException failure) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("PROFILE_FORBIDDEN",
                "No puedes editar el perfil de otro usuario", Map.of()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> userNotFound(UserNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("USER_NOT_FOUND",
                "Usuario no encontrado", Map.of()));
    }

    @ExceptionHandler(UserStatusConflictException.class)
    ResponseEntity<ApiError> userStatusConflict(UserStatusConflictException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("USER_STATUS_CONFLICT",
                failure.getMessage(), Map.of("status", failure.getCurrentStatus().name())));
    }

    @ExceptionHandler(SelfSuspensionException.class)
    ResponseEntity<ApiError> selfSuspension(SelfSuspensionException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("SELF_SUSPENSION_FORBIDDEN",
                "No puedes suspender tu propia cuenta", Map.of()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException failure) {
        Map<String, String> errors = new LinkedHashMap<>();
        failure.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            errors.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        });
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Datos invalidos", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException failure) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Datos invalidos",
                Map.of(failure.getName(), "Valor invalido")));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    ResponseEntity<ApiError> projectNotFound(ProjectNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("PROJECT_NOT_FOUND",
            "Proyecto no encontrado", Map.of()));
    }

    @ExceptionHandler(PostNotFoundException.class)
    ResponseEntity<ApiError> postNotFound(PostNotFoundException failure) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("POST_NOT_FOUND",
                "Publicacion no encontrada", Map.of()));
    }

    @ExceptionHandler(PostOwnershipException.class)
    ResponseEntity<ApiError> postOwnership(PostOwnershipException failure) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("POST_FORBIDDEN",
                "Solo el autor puede realizar esta accion sobre su publicacion", Map.of()));
    }

    @ExceptionHandler(PostTypeConflictException.class)
    ResponseEntity<ApiError> postTypeConflict(PostTypeConflictException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("POST_TYPE_CONFLICT",
                "Solo se pueden resolver discusiones tecnicas", Map.of()));
    }

    
    @ExceptionHandler(ProjectOwnershipException.class)
    ResponseEntity<ApiError> projectOwnership(ProjectOwnershipException failure) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("PROJECT_FORBIDDEN",
            "No puedes editar el proyecto de otro usuario", Map.of()));
    }

    @ExceptionHandler(ReportAlreadyExistsException.class)
    ResponseEntity<ApiError> reportAlreadyExists(ReportAlreadyExistsException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("REPORT_ALREADY_EXISTS",
            "Ya tienes un reporte pendiente sobre este elemento", Map.of()));
    }

    @ExceptionHandler(CannotReportOwnContentException.class)
    ResponseEntity<ApiError> cannotReportOwnContent(CannotReportOwnContentException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("OWN_CONTENT_REPORT",
                "No puedes reportar tu propio contenido", Map.of()));
    }
}
