package com.eav02.backend.auth.dto;

import com.eav02.backend.common.validation.BcryptPasswordLength;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, message = "Debe tener al menos 8 caracteres")
        @Pattern(regexp = "(?s).*[0-9].*", message = "Debe contener al menos un numero")
        @BcryptPasswordLength String password,
        @NotBlank String passwordConfirmation) {

    public RegistrationRequest {
        if (username != null) {
            username = username.trim();
        }
        if (email != null) {
            email = email.trim();
        }
    }

    @Override
    public String toString() {
        return "RegistrationRequest[redacted]";
    }
}
