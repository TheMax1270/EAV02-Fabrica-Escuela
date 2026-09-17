package com.eav02.backend.common.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BcryptPasswordLengthValidator implements ConstraintValidator<BcryptPasswordLength, String> {

    private static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isWithinLimit(value);
    }

    public static boolean isWithinLimit(String value) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
