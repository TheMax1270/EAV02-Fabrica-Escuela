package com.eav02.backend.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.ProjectNotFoundException;
import com.eav02.backend.common.exception.ProjectOwnershipException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.project.dto.ProjectRequest;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.project.entity.ProjectStatus;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    @Mock private ProjectRepository projects;
    @Mock private UserRepository users;

    private ProjectService service;
    private AppUser owner;
    private final UUID projectId = UUID.randomUUID();
    private final ProjectRequest request = new ProjectRequest("  Nuevo titulo ", " Nueva descripcion ",
            List.of(" Java ", "Spring"), ProjectStatus.COMPLETED, " https://github.com/devconnect/app ");

    @BeforeEach
    void setUp() {
        service = new ProjectService(projects, users, Clock.fixed(NOW, ZoneOffset.UTC));
        owner = new AppUser("Ada Lovelace", "ada", "ada@example.com", "hash", UserRole.DEVELOPER, true, CREATED);
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());
    }

    private Project existingProject() {
        var project = new Project(owner, "Titulo", "Descripcion", List.of("Kotlin"), ProjectStatus.IN_PROGRESS,
                null, CREATED);
        ReflectionTestUtils.setField(project, "id", projectId);
        return project;
    }

    @Test
    void createsAProjectForTheAuthenticatedUser() {
        when(users.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(projects.saveAndFlush(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(owner.getId(), request);

        var captor = ArgumentCaptor.forClass(Project.class);
        verify(projects).saveAndFlush(captor.capture());
        assertEquals(owner, captor.getValue().getUser());
        assertEquals("Nuevo titulo", response.title());
        assertEquals("Nueva descripcion", response.description());
        assertEquals(List.of("Java", "Spring"), response.technologies());
        assertEquals(ProjectStatus.COMPLETED, response.status());
        assertEquals("https://github.com/devconnect/app", response.repositoryUrl());
        assertEquals(owner.getId(), response.userId());
        assertEquals("ada", response.username());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW, response.updatedAt());
    }

    @Test
    void createFailsWhenTheUserDoesNotExist() {
        when(users.findById(owner.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.create(owner.getId(), request));
        verify(projects, never()).saveAndFlush(any());
    }

    @Test
    void returnsAProjectById() {
        when(projects.findWithUserById(projectId)).thenReturn(Optional.of(existingProject()));

        var response = service.getById(projectId);

        assertEquals(projectId, response.id());
        assertEquals("Titulo", response.title());
        assertEquals(CREATED, response.createdAt());
    }

    @Test
    void getByIdFailsWhenTheProjectDoesNotExist() {
        when(projects.findWithUserById(projectId)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.getById(projectId));
    }

    @Test
    void listsTheProjectsOfAUser() {
        when(projects.findAllWithUserByUserId(owner.getId())).thenReturn(List.of(existingProject()));

        var result = service.listByUser(owner.getId());

        assertEquals(1, result.size());
        assertEquals("Titulo", result.get(0).title());
    }

    @Test
    void listsNothingForAUserWithoutProjects() {
        when(projects.findAllWithUserByUserId(owner.getId())).thenReturn(List.of());

        assertTrue(service.listByUser(owner.getId()).isEmpty());
    }

    @Test
    void ownerCanUpdateTheProject() {
        var project = existingProject();
        when(projects.findWithUserById(projectId)).thenReturn(Optional.of(project));

        var response = service.update(projectId, owner.getId(), request);

        assertEquals("Nuevo titulo", project.getTitle());
        assertEquals("Nueva descripcion", project.getDescription());
        assertEquals(List.of("Java", "Spring"), project.getTechnologies());
        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        assertEquals("https://github.com/devconnect/app", project.getRepositoryUrl());
        assertEquals(CREATED, project.getCreatedAt());
        assertEquals(NOW, project.getUpdatedAt());
        assertEquals("Nuevo titulo", response.title());
    }

    @Test
    void nonOwnerCannotUpdateTheProject() {
        var project = existingProject();
        when(projects.findWithUserById(projectId)).thenReturn(Optional.of(project));

        assertThrows(ProjectOwnershipException.class, () -> service.update(projectId, UUID.randomUUID(), request));
        assertEquals("Titulo", project.getTitle());
    }

    @Test
    void updateFailsWhenTheProjectDoesNotExist() {
        when(projects.findWithUserById(projectId)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.update(projectId, owner.getId(), request));
    }
}
