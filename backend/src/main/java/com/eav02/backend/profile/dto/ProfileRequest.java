package com.eav02.backend.profile.dto;

import java.util.List;

import com.eav02.backend.profile.entity.ExperienceLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank @Size(max = 500) String biography,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 50) String> programmingLanguages,
        @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 80) String> technologies,
        @jakarta.validation.constraints.NotNull ExperienceLevel experienceLevel,
        @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL valida") String githubUrl,
        @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL valida") String linkedinUrl,
        @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL valida") String portfolioUrl) {

    public ProfileRequest {
        biography = biography == null ? null : biography.trim();
        programmingLanguages = normalize(programmingLanguages);
        technologies = normalize(technologies);
        githubUrl = normalizeUrl(githubUrl);
        linkedinUrl = normalizeUrl(linkedinUrl);
        portfolioUrl = normalizeUrl(portfolioUrl);
    }

    private static List<String> normalize(List<String> values) {
        return values == null ? null : values.stream().map(value -> value == null ? null : value.trim()).toList();
    }

    private static String normalizeUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
