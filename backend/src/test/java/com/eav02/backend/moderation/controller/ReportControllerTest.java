package com.eav02.backend.moderation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.eav02.backend.common.exception.CannotReportOwnContentException;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.ReportAlreadyExistsException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.moderation.dto.ReportRequest;
import com.eav02.backend.moderation.dto.ReportResponse;
import com.eav02.backend.moderation.dto.ReportStatusResponse;
import com.eav02.backend.moderation.entity.ReportReason;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.service.ReportService;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReportControllerTest {

    private static final String BODY = "{\"reason\":\"SPAM\",\"details\":\"  texto  \"}";

    @Autowired private MockMvc mvc;
    @MockitoBean private ReportService service;
    @MockitoBean private JwtDecoder decoder;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID userId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final ReportRequest expectedRequest = new ReportRequest(ReportReason.SPAM, "texto");

    private void authenticate() {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256").subject(userId.toString())
                .claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER"),
                        new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
    }

    private ReportResponse response(ReportTargetType type) {
        return new ReportResponse(UUID.randomUUID(), userId, "ada", type, targetId, ReportReason.SPAM, "texto",
                ReportStatus.PENDING, Instant.parse("2026-10-19T12:00:00Z"));
    }

    @Test
    void reportsAPost() throws Exception {
        authenticate();
        when(service.reportPost(targetId, userId, expectedRequest)).thenReturn(response(ReportTargetType.POST));

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("POST"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reporterUsername").value("ada"));
        verify(service).reportPost(targetId, userId, expectedRequest);
    }

    @Test
    void reportsAComment() throws Exception {
        authenticate();
        when(service.reportComment(targetId, userId, expectedRequest))
                .thenReturn(response(ReportTargetType.COMMENT));

        mvc.perform(post("/api/v1/comments/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("COMMENT"));
    }

    @Test
    void reportsAProfile() throws Exception {
        authenticate();
        when(service.reportProfile(targetId, userId, expectedRequest))
                .thenReturn(response(ReportTargetType.PROFILE));

        mvc.perform(post("/api/v1/profiles/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("PROFILE"));
    }

    @Test
    void rejectsAReportWithoutReason() throws Exception {
        authenticate();

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"details\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.reason").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAnUnknownReason() throws Exception {
        authenticate();

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void rejectsTooLongDetails() throws Exception {
        authenticate();

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"details\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.details").exists());
    }

    @Test
    void duplicatedReportReturnsConflict() throws Exception {
        authenticate();
        when(service.reportPost(targetId, userId, expectedRequest)).thenThrow(new ReportAlreadyExistsException());

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));
    }

    @Test
    void reportingOwnContentReturnsConflict() throws Exception {
        authenticate();
        when(service.reportComment(targetId, userId, expectedRequest))
                .thenThrow(new CannotReportOwnContentException());

        mvc.perform(post("/api/v1/comments/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWN_CONTENT_REPORT"));
    }

    @Test
    void reportingAMissingPostReturnsNotFound() throws Exception {
        authenticate();
        when(service.reportPost(targetId, userId, expectedRequest)).thenThrow(new PostNotFoundException());

        mvc.perform(post("/api/v1/posts/" + targetId + "/reports").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"posts", "comments", "profiles"})
    void creatingAReportRequiresAuthentication(String collection) throws Exception {
        mvc.perform(post("/api/v1/" + collection + "/" + targetId + "/reports")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void postStatusReflectsAPendingReport() throws Exception {
        authenticate();
        when(service.status(ReportTargetType.POST, targetId, userId)).thenReturn(new ReportStatusResponse(true));

        mvc.perform(get("/api/v1/posts/" + targetId + "/reports/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(true));
    }

    @Test
    void commentStatusReflectsAPendingReport() throws Exception {
        authenticate();
        when(service.status(ReportTargetType.COMMENT, targetId, userId)).thenReturn(new ReportStatusResponse(false));

        mvc.perform(get("/api/v1/comments/" + targetId + "/reports/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(false));
    }

    @Test
    void profileStatusReflectsAPendingReport() throws Exception {
        authenticate();
        when(service.status(eq(ReportTargetType.PROFILE), eq(targetId), eq(userId)))
                .thenReturn(new ReportStatusResponse(true));

        mvc.perform(get("/api/v1/profiles/" + targetId + "/reports/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(true));
    }

    /**
     * Regresion: GET /api/v1/profiles/** es publico y estaba declarado antes que la regla de
     * /profiles/{id}/reports/me, por lo que una peticion anonima llegaba al controlador sin JWT.
     */
    @ParameterizedTest
    @ValueSource(strings = {"posts", "comments", "profiles"})
    void reportStatusRequiresAuthentication(String collection) throws Exception {
        mvc.perform(get("/api/v1/" + collection + "/" + targetId + "/reports/me"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
