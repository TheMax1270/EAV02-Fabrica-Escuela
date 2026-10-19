package com.eav02.backend.interaction.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.eav02.backend.common.exception.SelfFollowException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.interaction.dto.FollowPageResponse;
import com.eav02.backend.interaction.dto.FollowSummaryResponse;
import com.eav02.backend.interaction.dto.FollowUserResponse;
import com.eav02.backend.interaction.service.FollowService;

@WebMvcTest(FollowController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FollowControllerTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer valid";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    FollowService follows;
    @MockitoBean
    JwtDecoder decoder;
    @MockitoBean
    TokenService tokens;
    @MockitoBean
    SessionJwtAuthenticationConverter converter;

    final UUID target = UUID.randomUUID();

    @Test
    void followsAndUnfollowsUsingJwtSubject() throws Exception {
        UUID user = authenticate();
        mvc.perform(put(meFollowPath()).header(AUTHORIZATION, BEARER)).andExpect(status().isNoContent());
        mvc.perform(put(meFollowPath()).header(AUTHORIZATION, BEARER)).andExpect(status().isNoContent());
        mvc.perform(delete(meFollowPath()).header(AUTHORIZATION, BEARER)).andExpect(status().isNoContent());
        verify(follows, times(2)).follow(user, target);
        verify(follows).unfollow(user, target);
    }

    @Test
    void anonymousCannotChangeFollows() throws Exception {
        mvc.perform(put(meFollowPath())).andExpect(status().isUnauthorized());
        mvc.perform(delete(meFollowPath())).andExpect(status().isUnauthorized());
        verifyNoInteractions(follows);
    }

    @Test
    void selfFollowReturnsConflict() throws Exception {
        UUID user = authenticate();
        doThrow(new SelfFollowException()).when(follows).follow(user, target);
        mvc.perform(put(meFollowPath()).header(AUTHORIZATION, BEARER)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_FOLLOW_FORBIDDEN"));
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        UUID user = authenticate();
        doThrow(new UserNotFoundException()).when(follows).follow(user, target);
        mvc.perform(put(meFollowPath()).header(AUTHORIZATION, BEARER)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void publicReadsSummaryWithoutCaching() throws Exception {
        when(follows.summary(target, null)).thenReturn(new FollowSummaryResponse(2, 4, null));
        mvc.perform(get(base() + "/follow-summary")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.followersCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(4));
    }

    @Test
    void authenticatedReadsPersonalSummary() throws Exception {
        UUID user = authenticate();
        when(follows.summary(target, user)).thenReturn(new FollowSummaryResponse(1, 0, true));
        mvc.perform(get(base() + "/follow-summary").header(AUTHORIZATION, BEARER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.followedByMe").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"followers", "following"})
    void publicCanListFollowRelations(String kind) throws Exception {
        var entry = new FollowUserResponse(UUID.randomUUID(), "ada", Instant.parse("2026-10-17T12:00:00Z"));
        var page = new FollowPageResponse(List.of(entry), 0, 20, 1, 1);
        when(follows.followers(target, 0, 20)).thenReturn(page);
        when(follows.following(target, 0, 20)).thenReturn(page);
        mvc.perform(get(base() + "/" + kind)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("ada"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get(base() + "/followers?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(follows);
    }

    private String base() {
        return "/api/v1/users/" + target;
    }

    private String meFollowPath() {
        return base() + "/followers/me";
    }

    private UUID authenticate() {
        UUID user = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256").subject(user.toString())
                .claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(converter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority("ROLE_DEVELOPER"),
                new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
        return user;
    }
}
