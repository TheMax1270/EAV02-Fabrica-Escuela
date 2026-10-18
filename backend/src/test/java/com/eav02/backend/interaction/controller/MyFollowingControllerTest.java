package com.eav02.backend.interaction.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.eav02.backend.interaction.dto.FollowedIdsResponse;
import com.eav02.backend.interaction.service.FollowService;

@WebMvcTest(MyFollowingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MyFollowingControllerTest {

    private static final String PATH = "/api/v1/me/following";
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

    @Test
    void returnsFollowedIdsForJwtSubject() throws Exception {
        UUID user = authenticate();
        UUID followed = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        when(follows.followedAmong(user, List.of(followed, other))).thenReturn(new FollowedIdsResponse(List.of(followed)));

        mvc.perform(get(PATH).param("userIds", followed + "," + other).header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.userIds[0]").value(followed.toString()))
                .andExpect(jsonPath("$.userIds.length()").value(1));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get(PATH).param("userIds", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        verifyNoInteractions(follows);
    }

    @Test
    void rejectsMoreThanOneHundredIds() throws Exception {
        authenticate();
        String ids = Stream.generate(UUID::randomUUID).limit(101).map(UUID::toString).collect(Collectors.joining(","));
        mvc.perform(get(PATH).param("userIds", ids).header(AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(follows);
    }

    @Test
    void rejectsInvalidIds() throws Exception {
        authenticate();
        mvc.perform(get(PATH).param("userIds", "abc").header(AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(follows);
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
