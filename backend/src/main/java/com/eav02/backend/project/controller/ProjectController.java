package com.eav02.backend.project.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.project.dto.ProjectRequest;
import com.eav02.backend.project.dto.ProjectResponse;
import com.eav02.backend.project.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(userId(jwt), request));
    }

    @GetMapping("/me")
    public List<ProjectResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return projectService.listByUser(userId(jwt));
    }

    @GetMapping("/user/{userId}")
    public List<ProjectResponse> byUser(@PathVariable UUID userId) {
        return projectService.listByUser(userId);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getById(@PathVariable UUID projectId) {
        return projectService.getById(projectId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectRequest request) {
        return projectService.update(projectId, userId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}