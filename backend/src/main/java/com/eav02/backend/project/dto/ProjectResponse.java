package com.eav02.backend.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eav02.backend.project.entity.Project;
import com.eav02.backend.project.entity.ProjectStatus;

public record ProjectResponse(UUID id, UUID userId, String username, String title, String description,
        List<String> technologies, ProjectStatus status, String repositoryUrl, Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getUser().getId(), project.getUser().getUsername(),
                project.getTitle(), project.getDescription(), project.getTechnologies(), project.getStatus(),
                project.getRepositoryUrl(), project.getCreatedAt(), project.getUpdatedAt());
    }
}