package com.eav02.backend.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.entity.AdminAuditLog;
import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.auth.entity.AuthSession;
import com.eav02.backend.common.exception.PostTypeConflictException;
import com.eav02.backend.interaction.dto.CommentRequest;
import com.eav02.backend.interaction.dto.CommentResponse;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.entity.PostType;
import com.eav02.backend.project.dto.ProjectRequest;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.project.entity.ProjectStatus;
import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;

/** Pruebas de entidades y DTOs con logica propia (sin Spring ni base de datos). */
class DomainModelTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser("Ada Lovelace", "ada", "ada@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    }

    private Post post() {
        var post = new Post(user, "Titulo", "Contenido", NOW);
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        return post;
    }

    // ---------- AppUser ----------

    @Test
    void suspendAndReactivateToggleTheAccountStatus() {
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        user.suspend();
        assertFalse(user.isEnabled());
        assertEquals(AccountStatus.SUSPENDED, user.getStatus());
        user.reactivate();
        assertTrue(user.isEnabled());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    // ---------- Post ----------

    @Test
    void newPostsAreGeneralVisibleAndUnresolved() {
        var post = post();

        assertEquals(PostType.GENERAL, post.getType());
        assertEquals(ModerationStatus.VISIBLE, post.getModerationStatus());
        assertFalse(post.isResolved());
        assertTrue(post.getCategories().isEmpty());
    }

    @Test
    void postModerationTransitions() {
        var post = post();
        post.markInReview();
        assertEquals(ModerationStatus.IN_REVIEW, post.getModerationStatus());
        post.remove();
        assertEquals(ModerationStatus.REMOVED, post.getModerationStatus());
        post.restore();
        assertEquals(ModerationStatus.VISIBLE, post.getModerationStatus());
    }

    @Test
    void onlyTechnicalDiscussionsCanBeResolved() {
        assertThrows(PostTypeConflictException.class, () -> post().resolve());

        var discussion = new Post(user, "T", "C", PostType.TECHNICAL_DISCUSSION, "https://example.com",
                List.of("Java"), NOW);
        discussion.resolve();

        assertTrue(discussion.isResolved());
        assertEquals("https://example.com", discussion.getLink());
        assertEquals(List.of("Java"), discussion.getCategories());
    }

    // ---------- Comment ----------

    @Test
    void commentRequiresExactlyOneTarget() {
        var project = new Project(user, "P", "D", List.of("Java"), ProjectStatus.IN_PROGRESS, null, NOW);

        assertThrows(IllegalArgumentException.class, () -> new Comment(user, null, null, null, "x", NOW));
        assertThrows(IllegalArgumentException.class, () -> new Comment(user, project, post(), null, "x", NOW));
    }

    @Test
    void replyMustBelongToTheSameContentAsItsParent() {
        var post = post();
        var parent = new Comment(user, null, post, null, "padre", NOW);
        var otherPost = post();

        var reply = new Comment(user, null, post, parent, "respuesta", NOW);

        assertEquals(parent, reply.getParent());
        assertThrows(IllegalArgumentException.class, () -> new Comment(user, null, otherPost, parent, "x", NOW));
    }

    @Test
    void commentModerationTransitions() {
        var comment = new Comment(user, null, post(), null, "hola", NOW);
        assertEquals(ModerationStatus.VISIBLE, comment.getModerationStatus());
        comment.markInReview();
        assertEquals(ModerationStatus.IN_REVIEW, comment.getModerationStatus());
        comment.remove();
        assertEquals(ModerationStatus.REMOVED, comment.getModerationStatus());
        comment.restore();
        assertEquals(ModerationStatus.VISIBLE, comment.getModerationStatus());
    }

    @Test
    void commentResponseHandlesPostAndProjectTargets() {
        var post = post();
        var parent = new Comment(user, null, post, null, "padre", NOW);
        ReflectionTestUtils.setField(parent, "id", UUID.randomUUID());
        var reply = new Comment(user, null, post, parent, "respuesta", NOW);

        var onPost = CommentResponse.from(reply, 3);
        assertEquals(post.getId(), onPost.postId());
        assertNull(onPost.projectId());
        assertEquals(parent.getId(), onPost.parentCommentId());
        assertEquals(3, onPost.replyCount());

        var project = new Project(user, "P", "D", List.of("Java"), ProjectStatus.COMPLETED, null, NOW);
        var onProject = CommentResponse.from(new Comment(user, project, null, null, "x", NOW), 0);
        assertNull(onProject.postId());
        assertNull(onProject.parentCommentId());
    }

    // ---------- AuthSession ----------

    @Test
    void sessionIsActiveUntilExpiredOrRevoked() {
        var session = new AuthSession(UUID.randomUUID(), user, "hash", NOW, NOW.plusSeconds(60));

        assertTrue(session.isActive(NOW));
        assertFalse(session.isActive(NOW.plusSeconds(60)));
        session.revoke(NOW.plusSeconds(10));
        assertFalse(session.isActive(NOW));
        assertEquals(NOW.plusSeconds(10), session.getRevokedAt());
    }

    @Test
    void rotatingReplacesTheRefreshTokenHash() {
        var session = new AuthSession(UUID.randomUUID(), user, "viejo", NOW, NOW.plusSeconds(60));
        session.rotate("nuevo");
        assertEquals("nuevo", session.getRefreshTokenHash());
    }

    // ---------- AdminAuditLog ----------

    @Test
    void auditLogCopiesDetailsAndStoresTheActionName() {
        var id = UUID.randomUUID();
        var log = new AdminAuditLog(id, user, AdminAction.CONTENT_REMOVED, "POST", id, Map.of("k", "v"), NOW);

        assertEquals("CONTENT_REMOVED", log.getAction());
        assertEquals(Map.of("k", "v"), log.getDetails());
        assertNull(new AdminAuditLog(id, user, AdminAction.USER_SUSPENDED, "USER", id, null, NOW).getDetails());
    }

    // ---------- DTOs ----------

    @Test
    void projectRequestNormalizesItsFields() {
        var request = new ProjectRequest(" T ", " D ", List.of(" Java ", "Spring"), ProjectStatus.COMPLETED, "   ");

        assertEquals("T", request.title());
        assertEquals("D", request.description());
        assertEquals(List.of("Java", "Spring"), request.technologies());
        assertNull(request.repositoryUrl());
    }

    @Test
    void projectRequestKeepsNullsSoBeanValidationCanReportThem() {
        var request = new ProjectRequest(null, null, null, null, null);

        assertNull(request.title());
        assertNull(request.description());
        assertNull(request.technologies());
    }

    @Test
    void commentRequestTrimsContent() {
        assertEquals("hola", new CommentRequest("  hola ").content());
        assertNull(new CommentRequest(null).content());
    }

    @Test
    void registrationRequestTrimsAndNeverPrintsSecrets() {
        var request = new RegistrationRequest("Ada", " ada ", " ada@example.com ", "Clave1234", "Clave1234");

        assertEquals("ada", request.username());
        assertEquals("ada@example.com", request.email());
        assertFalse(request.toString().contains("Clave1234"));
        assertNull(new RegistrationRequest("Ada", null, null, "Clave1234", "Clave1234").username());
    }
}
