package com.eav02.backend.common.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptPasswordLengthValidator.class)
public @interface BcryptPasswordLength {

    String message() default "La contrasena no debe superar 72 bytes UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
