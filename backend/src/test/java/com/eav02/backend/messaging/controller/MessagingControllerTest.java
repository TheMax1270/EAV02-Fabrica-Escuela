package com.eav02.backend.messaging.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.BlockedUserException;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.SelfMessageException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.messaging.dto.ConversationPageResponse;
import com.eav02.backend.messaging.dto.MessageRequest;
import com.eav02.backend.messaging.dto.MessageResponse;
import com.eav02.backend.messaging.dto.MessageSliceResponse;
import com.eav02.backend.messaging.dto.ParticipantResponse;
import com.eav02.backend.messaging.service.MessagingService;

@WebMvcTest(MessagingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MessagingControllerTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer valid";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    MessagingService messaging;
    @MockitoBean
    JwtDecoder decoder;
    @MockitoBean
    TokenService tokens;
    @MockitoBean
    SessionJwtAuthenticationConverter converter;

    final UUID participant = UUID.randomUUID();

    @Test
    void sendsMessageUsingJwtSubject() throws Exception {
        UUID user = authenticate();
        var response = new MessageResponse(UUID.randomUUID(), UUID.randomUUID(), user, participant, "Hola",
                Instant.parse("2026-11-10T12:00:00Z"));
        when(messaging.send(user, participant, new MessageRequest("Hola"))).thenReturn(response);

        mvc.perform(post(messagesPath()).header(AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" Hola \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value(user.toString()))
                .andExpect(jsonPath("$.recipientId").value(participant.toString()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void anonymousCannotUseMessaging() throws Exception {
        mvc.perform(get("/api/v1/conversations")).andExpect(status().isUnauthorized());
        mvc.perform(get(messagesPath())).andExpect(status().isUnauthorized());
        mvc.perform(post(messagesPath()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(messaging);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":null}", "{\"content\":\"\"}", "{\"content\":\" \\n\\t \"}"})
    void rejectsBlankMessageWithoutWriting(String body) throws Exception {
        authenticate();
        mvc.perform(post(messagesPath()).header(AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.content").exists());
        verifyNoInteractions(messaging);
    }

    @Test
    void rejectsMessageAboveLimit() throws Exception {
        authenticate();
        mvc.perform(post(messagesPath()).header(AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
        verifyNoInteractions(messaging);
    }

    @Test
    void blockedRelationReturnsForbidden() throws Exception {
        UUID user = authenticate();
        when(messaging.send(eq(user), eq(participant), any())).thenThrow(new BlockedUserException());

        mvc.perform(post(messagesPath()).header(AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_BLOCKED"));
    }

    @Test
    void selfMessageReturnsConflict() throws Exception {
        UUID user = authenticate();
        when(messaging.send(eq(user), eq(participant), any())).thenThrow(new SelfMessageException());

        mvc.perform(post(messagesPath()).header(AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_MESSAGE_FORBIDDEN"));
    }

    @Test
    void readsHistoryWithCursorWithoutCaching() throws Exception {
        UUID user = authenticate();
        UUID before = UUID.randomUUID();
        when(messaging.history(user, participant, before, null, 30))
                .thenReturn(new MessageSliceResponse(new ParticipantResponse(participant, "linus"), List.of(), true));

        mvc.perform(get(messagesPath() + "?before=" + before).header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.participant.username").value("linus"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void readsInbox() throws Exception {
        UUID user = authenticate();
        when(messaging.inbox(user, 0, 20)).thenReturn(new ConversationPageResponse(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/conversations").header(AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(messaging).inbox(user, 0, 20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"limit=0", "limit=101", "limit=abc", "before=not-a-uuid"})
    void rejectsInvalidHistoryParameters(String query) throws Exception {
        authenticate();
        mvc.perform(get(messagesPath() + "?" + query).header(AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(messaging);
    }

    @Test
    void rejectsInvalidParticipantId() throws Exception {
        authenticate();
        mvc.perform(get("/api/v1/conversations/not-a-uuid/messages").header(AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(messaging);
    }

    private String messagesPath() {
        return "/api/v1/conversations/" + participant + "/messages";
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
