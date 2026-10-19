package com.eav02.backend.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.ProjectNotFoundException;
import com.eav02.backend.common.exception.ProjectOwnershipException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.project.dto.ProjectRequest;
import com.eav02.backend.project.dto.ProjectResponse;
import com.eav02.backend.project.entity.ProjectStatus;
import com.eav02.backend.project.service.ProjectService;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProjectControllerTest {

    private static final String VALID_BODY = """
            {"title":" Mi proyecto ","description":"Descripcion","technologies":["Java"," Spring "],
             "status":"IN_PROGRESS","repositoryUrl":"https://github.com/devconnect/app"}""";

    @Autowired private MockMvc mvc;
    @MockitoBean private ProjectService service;
    @MockitoBean private JwtDecoder decoder;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private UUID authenticate() {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256").subject(userId.toString())
                .claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER"),
                        new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
        return userId;
    }

    private ProjectResponse response() {
        var now = Instant.parse("2026-10-19T12:00:00Z");
        return new ProjectResponse(projectId, userId, "ada", "Mi proyecto", "Descripcion", List.of("Java", "Spring"),
                ProjectStatus.IN_PROGRESS, "https://github.com/devconnect/app", now, now);
    }

    @Test
    void createsAProjectUsingTheJwtUser() throws Exception {
        authenticate();
        when(service.create(eq(userId), any(ProjectRequest.class))).thenReturn(response());

        mvc.perform(post("/api/v1/projects").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(service).create(userId, new ProjectRequest("Mi proyecto", "Descripcion", List.of("Java", "Spring"),
                ProjectStatus.IN_PROGRESS, "https://github.com/devconnect/app"));
    }

    @Test
    void rejectsInvalidProjectData() throws Exception {
        authenticate();

        mvc.perform(post("/api/v1/projects").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"description\":\"d\",\"technologies\":[],\"status\":\"COMPLETED\","
                        + "\"repositoryUrl\":\"https://example.com/x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.technologies").exists())
                .andExpect(jsonPath("$.errors.repositoryUrl").exists());
        verifyNoInteractions(service);
    }

    @Test
    void creatingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void listsMyProjects() throws Exception {
        authenticate();
        when(service.listByUser(userId)).thenReturn(List.of(response()));

        mvc.perform(get("/api/v1/projects/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Mi proyecto"));
    }

    @Test
    void myProjectsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/projects/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsProjectsOfAnyUserPublicly() throws Exception {
        when(service.listByUser(userId)).thenReturn(List.of(response()));

        mvc.perform(get("/api/v1/projects/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void returnsAProjectPublicly() throws Exception {
        when(service.getById(projectId)).thenReturn(response());

        mvc.perform(get("/api/v1/projects/" + projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));
    }

    @Test
    void returnsNotFoundForAnUnknownProject() throws Exception {
        when(service.getById(projectId)).thenThrow(new ProjectNotFoundException());

        mvc.perform(get("/api/v1/projects/" + projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void rejectsAMalformedProjectId() throws Exception {
        mvc.perform(get("/api/v1/projects/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ownerUpdatesTheProject() throws Exception {
        authenticate();
        when(service.update(eq(projectId), eq(userId), any(ProjectRequest.class))).thenReturn(response());

        mvc.perform(put("/api/v1/projects/" + projectId).header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mi proyecto"));
    }

    @Test
    void nonOwnerGetsForbiddenOnUpdate() throws Exception {
        authenticate();
        when(service.update(eq(projectId), eq(userId), any(ProjectRequest.class)))
                .thenThrow(new ProjectOwnershipException());

        mvc.perform(put("/api/v1/projects/" + projectId).header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_FORBIDDEN"));
    }

    @Test
    void updatingRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/projects/" + projectId).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
