package com.eav02.backend.interaction.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.eav02.backend.common.exception.SelfBlockException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.interaction.dto.BlockSummaryResponse;
import com.eav02.backend.interaction.service.BlockService;

@WebMvcTest(BlockController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class BlockControllerTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer valid";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    BlockService blocks;
    @MockitoBean
    JwtDecoder decoder;
    @MockitoBean
    TokenService tokens;
    @MockitoBean
    SessionJwtAuthenticationConverter converter;

    private final UUID target = UUID.randomUUID();

    @Test
    void blocksAndUnblocksTargetUsingJwtSubject() throws Exception {
        UUID user = authenticate();

        mvc.perform(put(base() + "/blocks/me").header(AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());
        mvc.perform(delete(base() + "/blocks/me").header(AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());

        verify(blocks).block(user, target);
        verify(blocks).unblock(user, target);
    }

    @Test
    void anonymousCannotChangeBlocks() throws Exception {
        mvc.perform(put(base() + "/blocks/me")).andExpect(status().isUnauthorized());
        mvc.perform(delete(base() + "/blocks/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(blocks);
    }

    @Test
    void selfBlockReturnsConflict() throws Exception {
        UUID user = authenticate();
        doThrow(new SelfBlockException()).when(blocks).block(user, target);

        mvc.perform(put(base() + "/blocks/me").header(AUTHORIZATION, BEARER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_BLOCK_FORBIDDEN"));
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        UUID user = authenticate();
        doThrow(new UserNotFoundException()).when(blocks).block(user, target);

        mvc.perform(put(base() + "/blocks/me").header(AUTHORIZATION, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void publicReadsSummaryWithoutCaching() throws Exception {
        when(blocks.summary(target, null)).thenReturn(new BlockSummaryResponse(false, false));

        mvc.perform(get(base() + "/block-summary"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.blockedByMe").value(false))
                .andExpect(jsonPath("$.blockedMe").value(false));
    }

    @Test
    void authenticatedReadsPersonalSummary() throws Exception {
        UUID user = authenticate();
        when(blocks.summary(target, user)).thenReturn(new BlockSummaryResponse(true, false));

        mvc.perform(get(base() + "/block-summary").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedByMe").value(true))
                .andExpect(jsonPath("$.blockedMe").value(false));
    }

    private String base() {
        return "/api/v1/users/" + target;
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
