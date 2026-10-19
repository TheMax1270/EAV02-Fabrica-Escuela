package com.eav02.backend.common.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BcryptPasswordLengthValidatorTest {

    private final BcryptPasswordLengthValidator validator = new BcryptPasswordLengthValidator();

    @Test
    void acceptsNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void acceptsExactly72AsciiBytes() {
        assertTrue(validator.isValid("a".repeat(72), null));
        assertTrue(BcryptPasswordLengthValidator.isWithinLimit("a".repeat(72)));
    }

    @Test
    void rejects73AsciiBytes() {
        assertFalse(validator.isValid("a".repeat(73), null));
    }

    @Test
    void countsBytesNotCharacters() {
        // 36 caracteres de 2 bytes = 72 bytes (valido); 37 = 74 bytes (invalido).
        assertTrue(BcryptPasswordLengthValidator.isWithinLimit("ñ".repeat(36)));
        assertFalse(BcryptPasswordLengthValidator.isWithinLimit("ñ".repeat(37)));
    }

    @Test
    void acceptsEmptyPassword() {
        assertTrue(BcryptPasswordLengthValidator.isWithinLimit(""));
    }
}
