package com.eav02.backend.project.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.ProjectNotFoundException;
import com.eav02.backend.common.exception.ProjectOwnershipException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.project.dto.ProjectRequest;
import com.eav02.backend.project.dto.ProjectResponse;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final UserRepository users;
    private final Clock clock;

    public ProjectService(ProjectRepository projects, UserRepository users, Clock clock) {
        this.projects = projects;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public ProjectResponse create(UUID userId, ProjectRequest request) {
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var project = new Project(user, request.title(), request.description(), request.technologies(),
                request.status(), request.repositoryUrl(), clock.instant());
        return ProjectResponse.from(projects.saveAndFlush(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID projectId) {
        return projects.findWithUserById(projectId).map(ProjectResponse::from)
                .orElseThrow(ProjectNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByUser(UUID userId) {
        return projects.findAllWithUserByUserId(userId).stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, ProjectRequest request) {
        var project = projects.findWithUserById(projectId).orElseThrow(ProjectNotFoundException::new);
        if (!project.getUser().getId().equals(userId)) {
            throw new ProjectOwnershipException();
        }
        project.update(request.title(), request.description(), request.technologies(), request.status(),
                request.repositoryUrl(), clock.instant());
        return ProjectResponse.from(project);
    }
}