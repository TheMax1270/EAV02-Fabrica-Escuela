package com.eav02.backend.moderation.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.ModerationStateConflictException;
import com.eav02.backend.common.exception.ReportNotFoundException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.moderation.dto.AdminReportPageResponse;
import com.eav02.backend.moderation.dto.AdminReportResponse;
import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.moderation.entity.ReportReason;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.service.ModerationService;

@WebMvcTest(ModerationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ModerationControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ModerationService service;
    @MockitoBean private JwtDecoder decoder;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID adminId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    private void authenticateAs(String role) {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256").subject(adminId.toString())
                .claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY),
                        new SimpleGrantedAuthority(role))));
    }

    private AdminReportResponse report(ReportStatus status, ModerationStatus moderation) {
        return new AdminReportResponse(reportId, UUID.randomUUID(), "ada", ReportTargetType.POST, UUID.randomUUID(),
                "Titulo", "grace", ReportReason.SPAM, "detalle", status, moderation,
                Instant.parse("2026-10-19T12:00:00Z"));
    }

    private AdminReportPageResponse page(AdminReportResponse item) {
        return new AdminReportPageResponse(List.of(item), 0, 20, 1, 1);
    }

    @Test
    void adminListsPendingReportsWithDefaultPaging() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.pending(0, 20)).thenReturn(page(report(ReportStatus.PENDING, ModerationStatus.VISIBLE)));

        mvc.perform(get("/api/v1/admin/reports").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetPreview").value("Titulo"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminListsResolvedReportsWithCustomPaging() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.resolved(2, 50)).thenReturn(page(report(ReportStatus.RESOLVED, ModerationStatus.REMOVED)));

        mvc.perform(get("/api/v1/admin/reports/resolved?page=2&size=50").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101"})
    void rejectsOutOfRangePaging(String query) throws Exception {
        authenticateAs("ROLE_ADMIN");

        mvc.perform(get("/api/v1/admin/reports?" + query).header("Authorization", "Bearer valid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void adminPutsContentInReview() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.review(reportId, adminId)).thenReturn(report(ReportStatus.PENDING, ModerationStatus.IN_REVIEW));

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/review").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("IN_REVIEW"));
        verify(service).review(reportId, adminId);
    }

    @Test
    void adminRestoresContent() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.restore(reportId, adminId)).thenReturn(report(ReportStatus.RESOLVED, ModerationStatus.VISIBLE));

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/restore").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void adminRemovesContent() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.remove(reportId, adminId)).thenReturn(report(ReportStatus.RESOLVED, ModerationStatus.REMOVED));

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/remove").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("REMOVED"));
    }

    @Test
    void adminDismissesAReport() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.dismiss(reportId, adminId)).thenReturn(report(ReportStatus.DISMISSED, ModerationStatus.VISIBLE));

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/dismiss").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    void invalidStateTransitionReturnsConflict() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.restore(reportId, adminId)).thenThrow(new ModerationStateConflictException());

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/restore").header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MODERATION_STATE_CONFLICT"));
    }

    @Test
    void unknownReportReturnsNotFound() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(service.dismiss(reportId, adminId)).thenThrow(new ReportNotFoundException());

        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/dismiss").header("Authorization", "Bearer valid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void nonAdminUsersAreForbidden() throws Exception {
        authenticateAs("ROLE_DEVELOPER");

        mvc.perform(get("/api/v1/admin/reports").header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/remove").header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousUsersAreUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/admin/reports")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/admin/reports/" + reportId + "/review")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
