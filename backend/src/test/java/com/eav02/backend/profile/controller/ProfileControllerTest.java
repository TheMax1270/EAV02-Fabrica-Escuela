package com.eav02.backend.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.ProfileNotFoundException;
import com.eav02.backend.common.exception.ProfileOwnershipException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.profile.dto.ProfileResponse;
import com.eav02.backend.profile.entity.ExperienceLevel;
import com.eav02.backend.profile.service.IProfileService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IProfileService profiles;

    @MockitoBean
    private JwtDecoder decoder;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SessionJwtAuthenticationConverter sessionConverter;

    private final UUID userId = UUID.randomUUID();

    @Test
    void publicProfileIsReachableWithoutAuthentication() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(profiles.getPublic(profileId)).thenReturn(sampleResponse(profileId));

        mvc.perform(get("/api/v1/profiles/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biography").value("Backend developer"));
    }

    @Test
    void publicProfileReturnsNotFoundWhenMissing() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(profiles.getPublic(profileId)).thenThrow(new ProfileNotFoundException());

        mvc.perform(get("/api/v1/profiles/" + profileId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void ownProfileEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/profile/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/profile/me").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsOwnProfileWhenAuthenticated() throws Exception {
        authenticateAs(userId);
        when(profiles.create(eq(userId), any())).thenReturn(sampleResponse(UUID.randomUUID()));

        mvc.perform(post("/api/v1/profile/me").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.experienceLevel").value("SEMI_SENIOR"));
    }

    @Test
    void updatingSomeoneElsesProfileIsForbidden() throws Exception {
        authenticateAs(userId);
        UUID profileId = UUID.randomUUID();
        when(profiles.update(eq(profileId), eq(userId), any())).thenThrow(new ProfileOwnershipException());

        mvc.perform(put("/api/v1/profiles/" + profileId).header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_FORBIDDEN"));
    }

    private void authenticateAs(UUID subject) {
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject(subject.toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
    }

    private ProfileResponse sampleResponse(UUID profileId) {
        return new ProfileResponse(profileId, userId, "ada", "Backend developer",
                List.of("Java"), List.of("Spring"), ExperienceLevel.SEMI_SENIOR,
                "https://github.com/ada", null, null, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private String validJson() {
        return """
                {"biography":"Backend developer","programmingLanguages":["Java"],
                 "technologies":["Spring"],"experienceLevel":"SEMI_SENIOR",
                 "githubUrl":"https://github.com/ada"}
                """;
    }
}