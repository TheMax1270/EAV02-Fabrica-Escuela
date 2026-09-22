package com.eav02.backend.project.dto;

import java.util.List;

import com.eav02.backend.project.entity.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 50) String> technologies,
        @NotNull ProjectStatus status,
        @Size(max = 500)
        @Pattern(regexp = "^https?://([\\w-]+\\.)?(github|gitlab)\\.com/.+",
                message = "Debe ser una URL valida de GitHub o GitLab")
        String repositoryUrl) {

    public ProjectRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
        technologies = normalize(technologies);
        repositoryUrl = normalizeUrl(repositoryUrl);
    }

    private static List<String> normalize(List<String> values) {
        return values == null ? null : values.stream().map(value -> value == null ? null : value.trim()).toList();
    }

    private static String normalizeUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}