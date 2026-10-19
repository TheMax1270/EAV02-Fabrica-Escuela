package com.eav02.backend.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eav02.backend.auth.service.TokenService;
import com.eav02.backend.common.exception.GlobalExceptionHandler;
import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.PostOwnershipException;
import com.eav02.backend.common.exception.PostTypeConflictException;
import com.eav02.backend.common.security.SecurityConfig;
import com.eav02.backend.common.security.SessionJwtAuthenticationConverter;
import com.eav02.backend.post.dto.PostPageResponse;
import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.dto.PostResponse;
import com.eav02.backend.post.service.PostService;
import com.eav02.backend.post.entity.PostType;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PostControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private PostService service;
    @MockitoBean private JwtDecoder decoder;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SessionJwtAuthenticationConverter sessionConverter;

    @Test
    void createsUsingJwtAuthorEvenWhenBodyContainsAnotherUserId() throws Exception {
        UUID authorId = authenticate();
        var response = response(authorId);
        when(service.create(eq(authorId), any(PostRequest.class))).thenReturn(response);
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\" Java \",\"content\":\" Linea 1\\nLinea 2 \",\"userId\":\""
                        + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(service).create(authorId, new PostRequest("Java", "Linea 1\nLinea 2"));
    }

    @Test
    void anonymousCannotCreate() throws Exception {
        mvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java\",\"content\":\"Contenido\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectedSessionCannotCreate() throws Exception {
        var jwt = Jwt.withTokenValue("revoked").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).build();
        when(decoder.decode("revoked")).thenReturn(jwt);
        when(sessionConverter.convert(jwt))
                .thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer revoked")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java\",\"content\":\"Contenido\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null,\"content\":null}",
            "{\"title\":\"\",\"content\":\"\"}",
            "{\"title\":\"  \",\"content\":\" \\n\\t \"}"})
    void rejectsMissingNullEmptyAndWhitespaceFields(String body) throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content(body.equals("{}")
                        ? "{\"type\":\"TECHNICAL_DISCUSSION\"}"
                        : body.replaceFirst("\\{", "{\"type\":\"TECHNICAL_DISCUSSION\",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "content"})
    void rejectsOversizedFields(String field) throws Exception {
        authenticate();
        String title = field.equals("title") ? "a".repeat(151) : "Java";
        String content = field.equals("content") ? "a".repeat(5001) : "Contenido";
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 150})
    void acceptsLengthBoundaries(int titleLength) throws Exception {
        UUID authorId = authenticate();
        String title = "a".repeat(titleLength);
        String content = "b".repeat(titleLength == 1 ? 1 : 5000);
        when(service.create(eq(authorId), any(PostRequest.class))).thenReturn(response(authorId));
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated());
        verify(service).create(authorId, new PostRequest(title, content));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
        verifyNoInteractions(service);
    }

    @Test
    void anonymousCanReadDetail() throws Exception {
        var response = response(UUID.randomUUID());
        when(service.getById(response.id())).thenReturn(response);
        mvc.perform(get("/api/v1/posts/" + response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void missingPostReturnsApiError() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenThrow(new PostNotFoundException());
        mvc.perform(get("/api/v1/posts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Publicacion no encontrada"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void anonymousCanListWithDefaultPagination() throws Exception {
        when(service.list(0, 20)).thenReturn(new PostPageResponse(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(service).list(0, 20);
    }

    @Test
    void returnsRequestedPageAndMetadata() throws Exception {
        var response = response(UUID.randomUUID());
        when(service.list(1, 2)).thenReturn(new PostPageResponse(List.of(response), 1, 2, 3, 2));
        mvc.perform(get("/api/v1/posts").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        verify(service).list(1, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "size=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get("/api/v1/posts?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsPageSizeBoundaries(int size) throws Exception {
        when(service.list(0, size)).thenReturn(new PostPageResponse(List.of(), 0, size, 0, 0));
        mvc.perform(get("/api/v1/posts").param("size", Integer.toString(size)))
                .andExpect(status().isOk());
        verify(service).list(0, size);
    }

    @Test
    void rejectsInvalidPostUuid() throws Exception {
        mvc.perform(get("/api/v1/posts/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    private UUID authenticate() {
        UUID id = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("valid").header("alg", "HS256")
                .subject(id.toString()).claim("sid", UUID.randomUUID().toString()).build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER"),
                        new SimpleGrantedAuthority(FactorGrantedAuthority.BEARER_AUTHORITY))));
        return id;
    }

    @Test
    void authorCanDeleteUsingJwt() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/posts/" + id)
                .header("Authorization", "Bearer valid")).andExpect(status().isNoContent());
        verify(service).delete(id, author);
    }

    @Test
    void anonymousCannotDeletePost() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/posts/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void deletingAnotherAuthorsPostReturns403() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new PostOwnershipException()).when(service).delete(id, author);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/posts/" + id)
                .header("Authorization", "Bearer valid")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    void deletingMissingPostReturns404() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new PostNotFoundException()).when(service).delete(id, author);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/posts/" + id)
                .header("Authorization", "Bearer valid")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"link\":\"https://example.com/release\""})
    void createsGeneralWithoutTitle(String optionalLink) throws Exception {
        UUID author = authenticate();
        var result = new PostResponse(UUID.randomUUID(), author, "ada", null, "Lanzamiento",
                Instant.parse("2026-10-14T12:00:00Z"), PostType.GENERAL,
                optionalLink.isEmpty() ? null : "https://example.com/release", List.of(), false);
        when(service.create(eq(author), any(PostRequest.class))).thenReturn(result);
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"GENERAL\",\"content\":\"Lanzamiento\"" + optionalLink + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.resolved").value(false));
        verify(service).create(author, new PostRequest(null, "Lanzamiento", PostType.GENERAL,
                result.link(), List.of()));
    }

    @Test
    void createsTechnicalDiscussionWithCategory() throws Exception {
        UUID author = authenticate();
        when(service.create(eq(author), any(PostRequest.class))).thenReturn(technicalResponse(author, false));
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TECHNICAL_DISCUSSION\",\"title\":\"Spring\",\"content\":\"Problema\",\"categories\":[\" Java \"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TECHNICAL_DISCUSSION"))
                .andExpect(jsonPath("$.categories[0]").value("Java"))
                .andExpect(jsonPath("$.resolved").value(false));
        verify(service).create(author, new PostRequest("Spring", "Problema", PostType.TECHNICAL_DISCUSSION, null, List.of("Java")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"categories\":[]", ",\"categories\":null",
            ",\"categories\":[\"  \"]", ",\"categories\":[null]"})
    void rejectsTechnicalDiscussionWithoutValidCategory(String categories) throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TECHNICAL_DISCUSSION\",\"title\":\"Spring\",\"content\":\"Problema\"" + categories + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void missingCategoryHasClearFieldError() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TECHNICAL_DISCUSSION\",\"title\":\"Spring\",\"content\":\"Problema\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categories").value("Indica al menos una categoria o tecnologia"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "javascript:alert(1)", "https://", "https://example.com/a b", "https://user:password@example.com"})
    void rejectsInvalidLinks(String link) throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"GENERAL\",\"content\":\"Texto\",\"link\":\"" + link + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.link").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownType() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/posts").header("Authorization", "Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"OTHER\",\"content\":\"Texto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
        verifyNoInteractions(service);
    }

    @Test
    void resolvesWithJwtAuthor() throws Exception {
        UUID author = authenticate();
        var result = technicalResponse(author, true);
        when(service.resolve(result.id(), author)).thenReturn(result);
        mvc.perform(patch("/api/v1/posts/" + result.id() + "/resolve").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved").value(true));
        verify(service).resolve(result.id(), author);
    }

    @Test
    void anonymousCannotResolve() throws Exception {
        mvc.perform(patch("/api/v1/posts/" + UUID.randomUUID() + "/resolve"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectedSessionCannotResolve() throws Exception {
        var jwt = Jwt.withTokenValue("revoked").header("alg", "HS256").subject(UUID.randomUUID().toString()).build();
        when(decoder.decode("revoked")).thenReturn(jwt);
        when(sessionConverter.convert(jwt)).thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
        mvc.perform(patch("/api/v1/posts/" + UUID.randomUUID() + "/resolve").header("Authorization", "Bearer revoked"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void cannotResolveAnotherAuthorsPost() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        when(service.resolve(id, author)).thenThrow(new PostOwnershipException());
        mvc.perform(patch("/api/v1/posts/" + id + "/resolve").header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    void cannotResolveGeneralPost() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        when(service.resolve(id, author)).thenThrow(new PostTypeConflictException());
        mvc.perform(patch("/api/v1/posts/" + id + "/resolve").header("Authorization", "Bearer valid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_TYPE_CONFLICT"));
    }

    @Test
    void resolvingMissingPostReturns404() throws Exception {
        UUID author = authenticate();
        UUID id = UUID.randomUUID();
        when(service.resolve(id, author)).thenThrow(new PostNotFoundException());
        mvc.perform(patch("/api/v1/posts/" + id + "/resolve").header("Authorization", "Bearer valid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void allowsPatchCorsPreflight() throws Exception {
        mvc.perform(options("/api/v1/posts/" + UUID.randomUUID() + "/resolve")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void publicListAndDetailExposeBothTypes() throws Exception {
        var general = response(UUID.randomUUID());
        var technical = technicalResponse(UUID.randomUUID(), true);
        when(service.list(0, 20)).thenReturn(new PostPageResponse(List.of(general, technical), 0, 20, 2, 1));
        when(service.getById(technical.id())).thenReturn(technical);
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("GENERAL"))
                .andExpect(jsonPath("$.content[1].type").value("TECHNICAL_DISCUSSION"));
        mvc.perform(get("/api/v1/posts/" + technical.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0]").value("Java"))
                .andExpect(jsonPath("$.resolved").value(true));
    }

    private PostResponse technicalResponse(UUID author, boolean resolved) {
        return new PostResponse(UUID.randomUUID(), author, "ada", "Spring", "Problema",
                Instant.parse("2026-10-14T12:00:00Z"), PostType.TECHNICAL_DISCUSSION, null, List.of("Java"), resolved);
    }

    private PostResponse response(UUID authorId) {
        return new PostResponse(UUID.randomUUID(), authorId, "ada", "Java", "Contenido",
                Instant.parse("2026-10-14T12:00:00Z"));
    }
}
