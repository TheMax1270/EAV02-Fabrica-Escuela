package com.eav02.backend.statistics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.statistics.dto.StatisticsGroupBy;
import com.eav02.backend.statistics.dto.StatisticsPeriod;
import com.eav02.backend.statistics.dto.StatisticsResponse;
import com.eav02.backend.statistics.dto.StatisticsTotals;
import com.eav02.backend.statistics.exception.InvalidStatisticsRequestException;
import com.eav02.backend.statistics.service.StatisticsService;

@WebMvcTest(StatisticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StatisticsService statistics;

    @MockitoBean
    private JwtDecoder decoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/statistics")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void developerRequestIsForbidden() throws Exception {
        authenticateAs("ROLE_DEVELOPER");

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsAllStatisticsWithWeeklySeries() throws Exception {
        authenticateAs("ROLE_ADMIN");
        var response = new StatisticsResponse(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"),
                StatisticsGroupBy.WEEK,
                new StatisticsTotals(4, 1, 2, 7),
                List.of(new StatisticsPeriod("2025-12-29", 4, 1, 2, 7)));
        when(statistics.getStatistics(
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), StatisticsGroupBy.WEEK))
                .thenReturn(response);

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .param("groupBy", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                .andExpect(jsonPath("$.endDate").value("2026-01-31"))
                .andExpect(jsonPath("$.groupBy").value("WEEK"))
                .andExpect(jsonPath("$.totals.users").value(4))
                .andExpect(jsonPath("$.totals.projects").value(1))
                .andExpect(jsonPath("$.totals.posts").value(2))
                .andExpect(jsonPath("$.totals.comments").value(7))
                .andExpect(jsonPath("$.series[0].period").value("2025-12-29"))
                .andExpect(jsonPath("$.series[0].users").value(4))
                .andExpect(jsonPath("$.series[0].projects").value(1))
                .andExpect(jsonPath("$.series[0].posts").value(2))
                .andExpect(jsonPath("$.series[0].comments").value(7));
    }

    @Test
    void defaultsToMonthlyGroupingWhenParameterIsOmitted() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(statistics.getStatistics(
                LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"), StatisticsGroupBy.MONTH))
                .thenReturn(new StatisticsResponse(
                        LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"),
                        StatisticsGroupBy.MONTH, new StatisticsTotals(0, 0, 0, 0), List.of()));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("MONTH"))
                .andExpect(jsonPath("$.totals.users").value(0))
                .andExpect(jsonPath("$.series").isEmpty());

        verify(statistics).getStatistics(
                LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"), StatisticsGroupBy.MONTH);
    }

    @Test
    void invalidDatesAndGroupingReturnBadRequest() throws Exception {
        authenticateAs("ROLE_ADMIN");

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-02-30")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.startDate").value("Debe ser una fecha válida"));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026/02/01")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").value("Debe tener formato YYYY-MM-DD"));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-03-01")
                        .param("groupBy", "DAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.groupBy").value("Debe ser WEEK o MONTH"));
    }

    @Test
    void missingDatesAndInvertedRangeReturnBadRequest() throws Exception {
        authenticateAs("ROLE_ADMIN");
        when(statistics.getStatistics(
                LocalDate.parse("2026-03-02"), LocalDate.parse("2026-03-01"), StatisticsGroupBy.MONTH))
                .thenThrow(new InvalidStatisticsRequestException("startDate", "No puede ser posterior a endDate"));
        when(statistics.getStatistics(
                LocalDate.parse("2021-01-01"), LocalDate.parse("2026-01-02"), StatisticsGroupBy.MONTH))
                .thenThrow(new InvalidStatisticsRequestException("endDate", "El rango no puede superar cinco años"));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.endDate").value("Este parametro es obligatorio"));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2026-03-02")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").value("No puede ser posterior a endDate"));

        mvc.perform(get("/api/v1/admin/statistics")
                        .header("Authorization", "Bearer valid")
                        .param("startDate", "2021-01-01")
                        .param("endDate", "2026-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.endDate").value("El rango no puede superar cinco años"));
    }

    private void authenticateAs(String role) {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject(adminId.toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY),
                        new SimpleGrantedAuthority(role))));
    }
}
