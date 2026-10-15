package com.eav02.backend.interaction.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.security.*;
import com.eav02.backend.common.exception.*;
import com.eav02.backend.interaction.dto.*;
import com.eav02.backend.interaction.service.*;

@WebMvcTest({CommentController.class, ReactionController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InteractionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CommentService comments;
    @MockitoBean ReactionService reactions;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean TokenService tokens;
    @MockitoBean SessionJwtAuthenticationConverter converter;
    UUID id = UUID.randomUUID();
    @ParameterizedTest @ValueSource(strings = {"projects", "posts", "comments"})
    void createsUsingJwtAuthorIgnoringInjectedAuthor(String kind) throws Exception {
        UUID user = authenticate();
        var result = response(user, kind);
        when(comments.commentProject(eq(id), eq(user), any())).thenReturn(result);
        when(comments.commentPost(eq(id), eq(user), any())).thenReturn(result);
        when(comments.reply(eq(id), eq(user), any())).thenReturn(result);
        mvc.perform(post(path(kind)).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" Texto\\nsegunda linea \",\"userId\":\"" + UUID.randomUUID() + "\",\"createdAt\":\"2000-01-01\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        var request = new CommentRequest("Texto\nsegunda linea");
        if (kind.equals("projects")) verify(comments).commentProject(id, user, request);
        else if (kind.equals("posts")) verify(comments).commentPost(id, user, request);
        else verify(comments).reply(id, user, request);
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"content\":null}", "{\"content\":\"\"}", "{\"content\":\" \\n\\t \"}"})
    void rejectsBlankWithoutWriting(String body) throws Exception {
        authenticate();
        mvc.perform(post(path("posts")).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
        verifyNoInteractions(comments);
    }
    @Test void rejectsContentAboveLimit() throws Exception {
        authenticate();
        mvc.perform(post(path("projects")).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
        verifyNoInteractions(comments);
    }
    @Test void acceptsContentAtLimit() throws Exception {
        UUID user = authenticate();
        when(comments.commentPost(eq(id), eq(user), any())).thenReturn(response(user, "posts"));
        mvc.perform(post(path("posts")).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "a".repeat(2000) + "\"}")).andExpect(status().isCreated());
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts", "comments"})
    void publicCanReadComments(String kind) throws Exception {
        var page = new CommentPageResponse(List.of(), 0, 20, 0, 0);
        when(comments.listProject(id, 0, 20)).thenReturn(page);
        when(comments.listPost(id, 0, 20)).thenReturn(page);
        when(comments.listReplies(id, 0, 20)).thenReturn(page);
        mvc.perform(get(path(kind))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts", "comments"})
    void anonymousCannotWriteComments(String kind) throws Exception {
        mvc.perform(post(path(kind)).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Texto\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(comments);
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts"})
    void putsAndDeletesOwnLikeIdempotently(String kind) throws Exception {
        UUID user = authenticate();
        String path = "/api/v1/" + kind + "/" + id + "/likes/me";
        mvc.perform(put(path).header("Authorization", "Bearer valid")).andExpect(status().isNoContent());
        mvc.perform(put(path).header("Authorization", "Bearer valid")).andExpect(status().isNoContent());
        mvc.perform(delete(path).header("Authorization", "Bearer valid")).andExpect(status().isNoContent());
        mvc.perform(delete(path).header("Authorization", "Bearer valid")).andExpect(status().isNoContent());
        verify(reactions, times(2)).like(id, kind.equals("projects"), user);
        verify(reactions, times(2)).unlike(id, kind.equals("projects"), user);
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts"})
    void anonymousCannotChangeLikes(String kind) throws Exception {
        String path = "/api/v1/" + kind + "/" + id + "/likes/me";
        mvc.perform(put(path)).andExpect(status().isUnauthorized());
        mvc.perform(delete(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(reactions);
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts"})
    void publicReadsSummaryWithoutCaching(String kind) throws Exception {
        when(reactions.summary(id, kind.equals("projects"), null)).thenReturn(new ReactionSummaryResponse(3, null));
        mvc.perform(get("/api/v1/" + kind + "/" + id + "/likes")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.likeCount").value(3));
    }
    @Test void authenticatedReadsPersonalSummary() throws Exception {
        UUID user = authenticate();
        when(reactions.summary(id, false, user)).thenReturn(new ReactionSummaryResponse(2, true));
        mvc.perform(get("/api/v1/posts/" + id + "/likes").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likedByMe").value(true));
    }
    @Test void replyToReplyReturnsFunctionalConflict() throws Exception {
        UUID user = authenticate();
        when(comments.reply(eq(id), eq(user), any())).thenThrow(new ReplyDepthException());
        mvc.perform(post(path("comments")).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Respuesta\"}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPLY_DEPTH_EXCEEDED"));
    }
    @ParameterizedTest @ValueSource(strings = {"projects", "posts", "comments"})
    void missingTargetsReturn404(String kind) throws Exception {
        UUID user = authenticate();
        when(comments.commentProject(eq(id), eq(user), any())).thenThrow(new ProjectNotFoundException());
        when(comments.commentPost(eq(id), eq(user), any())).thenThrow(new PostNotFoundException());
        when(comments.reply(eq(id), eq(user), any())).thenThrow(new CommentNotFoundException());
        mvc.perform(post(path(kind)).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Respuesta\"}")).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get(path("posts") + "?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(comments);
    }
    String path(String kind) { return "/api/v1/" + kind + "/" + id + (kind.equals("comments") ? "/replies" : "/comments"); }
    UUID authenticate() {
        UUID user = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("valid").header("alg", "HS256").subject(user.toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(converter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority("ROLE_DEVELOPER"), new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
        return user;
    }
    CommentResponse response(UUID user, String kind) {
        return new CommentResponse(UUID.randomUUID(), user, "ada", kind.equals("posts") ? null : id,
                kind.equals("posts") ? id : null, kind.equals("comments") ? id : null, "Texto", Instant.parse("2026-10-14T12:00:00Z"), 0);
    }
}
