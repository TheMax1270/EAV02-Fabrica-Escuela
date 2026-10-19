package com.eav02.backend.moderation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.service.AdminAuditService;
import com.eav02.backend.common.exception.ModerationStateConflictException;
import com.eav02.backend.common.exception.ModerationTargetNotFoundException;
import com.eav02.backend.common.exception.ReportNotFoundException;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportReason;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.repository.ReportRepository;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.profile.entity.DeveloperProfile;
import com.eav02.backend.profile.entity.ExperienceLevel;
import com.eav02.backend.profile.repository.DeveloperProfileRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    @Mock private ReportRepository reports;
    @Mock private PostRepository posts;
    @Mock private CommentRepository comments;
    @Mock private DeveloperProfileRepository profiles;
    @Mock private UserRepository users;
    @Mock private AdminAuditService audit;

    private ModerationService service;
    private AppUser admin;
    private AppUser reporter;
    private AppUser author;
    private final UUID reportId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    /** Acceso uniforme al estado de moderacion del contenido reportado. */
    private record Fixture(Supplier<ModerationStatus> status, Runnable markInReview, Runnable markRemoved) { }

    @BeforeEach
    void setUp() {
        service = new ModerationService(reports, posts, comments, profiles, users, audit);
        admin = user("admin", UserRole.ADMIN);
        reporter = user("reportante", UserRole.DEVELOPER);
        author = user("autor", UserRole.DEVELOPER);
    }

    private static AppUser user(String username, UserRole role) {
        var user = new AppUser("Nombre " + username, username, username + "@example.com", "hash", role, true, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Report report(ReportTargetType type) {
        var report = new Report(reporter, type, targetId, ReportReason.SPAM, "detalle", NOW);
        ReflectionTestUtils.setField(report, "id", reportId);
        return report;
    }

    private Fixture stubTarget(ReportTargetType type) {
        return switch (type) {
            case POST -> {
                var post = new Post(author, "Titulo", "Contenido", NOW);
                when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post));
                yield new Fixture(post::getModerationStatus, post::markInReview, post::remove);
            }
            case COMMENT -> {
                var parentPost = new Post(author, "Titulo", "Contenido", NOW);
                var comment = new Comment(author, null, parentPost, null, "Comentario", NOW);
                when(comments.findWithTargetById(targetId)).thenReturn(Optional.of(comment));
                yield new Fixture(comment::getModerationStatus, comment::markInReview, comment::remove);
            }
            case PROFILE -> {
                var profile = new DeveloperProfile(author, "Biografia", List.of("Java"), List.of("Spring"),
                        ExperienceLevel.SENIOR, null, null, null, NOW);
                when(profiles.findWithUserById(targetId)).thenReturn(Optional.of(profile));
                yield new Fixture(profile::getModerationStatus, profile::markInReview, profile::remove);
            }
        };
    }

    private Report stubPendingReport(ReportTargetType type) {
        var report = report(type);
        when(reports.findWithReporterById(reportId)).thenReturn(Optional.of(report));
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        return report;
    }

    private void verifyAudit(AdminAction action, ReportTargetType type) {
        verify(audit).record(eq(admin), eq(action), eq(type.name()), eq(targetId),
                eq(Map.<String, Object>of("reportId", reportId.toString())));
    }

    // ---------- listados ----------

    @Test
    void listsPendingReportsNewestFirst() {
        var report = report(ReportTargetType.POST);
        stubTarget(ReportTargetType.POST);
        Page<Report> page = new PageImpl<>(List.of(report), PageRequest.of(1, 5), 11);
        when(reports.findByStatusWithReporter(eq(ReportStatus.PENDING), any(Pageable.class))).thenReturn(page);

        var result = service.pending(1, 5);

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reports).findByStatusWithReporter(eq(ReportStatus.PENDING), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt", "id"), captor.getValue().getSort());
        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(11, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(1, result.content().size());
        var item = result.content().get(0);
        assertEquals("Titulo", item.targetPreview());
        assertEquals("autor", item.targetUsername());
        assertEquals("reportante", item.reporterUsername());
        assertEquals(ModerationStatus.VISIBLE, item.moderationStatus());
    }

    @Test
    void listsResolvedAndDismissedReports() {
        var report = report(ReportTargetType.COMMENT);
        stubTarget(ReportTargetType.COMMENT);
        when(reports.findByStatusesWithReporter(eq(List.of(ReportStatus.RESOLVED, ReportStatus.DISMISSED)),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        var result = service.resolved(0, 20);

        assertEquals(1, result.totalElements());
        assertEquals("Comentario", result.content().get(0).targetPreview());
    }

    @Test
    void previewsThePostContentWhenItHasNoTitle() {
        var report = report(ReportTargetType.POST);
        var post = new Post(author, null, "Solo contenido", NOW);
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post));
        when(reports.findByStatusWithReporter(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        assertEquals("Solo contenido", service.pending(0, 20).content().get(0).targetPreview());
    }

    @Test
    void previewsTheProfileBiography() {
        var report = report(ReportTargetType.PROFILE);
        stubTarget(ReportTargetType.PROFILE);
        when(reports.findByStatusWithReporter(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        assertEquals("Biografia", service.pending(0, 20).content().get(0).targetPreview());
    }

    @Test
    void listingToleratesReportsWhoseTargetNoLongerExists() {
        var report = report(ReportTargetType.POST);
        when(posts.findWithUserById(targetId)).thenReturn(Optional.empty());
        when(reports.findByStatusWithReporter(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        var item = service.pending(0, 20).content().get(0);

        assertEquals("El elemento reportado ya no existe", item.targetPreview());
        assertNull(item.targetUsername());
        assertNull(item.moderationStatus());
        assertEquals(ReportStatus.PENDING, item.status());
    }

    // ---------- review ----------

    @ParameterizedTest
    @EnumSource(ReportTargetType.class)
    void reviewMovesVisibleContentToInReviewKeepingTheReportPending(ReportTargetType type) {
        var report = stubPendingReport(type);
        var target = stubTarget(type);

        var result = service.review(reportId, admin.getId());

        assertEquals(ModerationStatus.IN_REVIEW, target.status().get());
        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertEquals(ModerationStatus.IN_REVIEW, result.moderationStatus());
        verifyAudit(AdminAction.CONTENT_IN_REVIEW, type);
        verify(reports).flush();
    }

    @Test
    void reviewFailsWhenContentIsNotVisible() {
        stubPendingReport(ReportTargetType.POST);
        stubTarget(ReportTargetType.POST).markInReview().run();

        assertThrows(ModerationStateConflictException.class, () -> service.review(reportId, admin.getId()));
        verify(audit, never()).record(any(), any(), any(), any(), any());
    }

    // ---------- restore ----------

    @Test
    void restoringAPostRestoresItsCommentsAndResolvesTheReport() {
        var report = stubPendingReport(ReportTargetType.POST);
        var target = stubTarget(ReportTargetType.POST);
        target.markInReview().run();

        var result = service.restore(reportId, admin.getId());

        assertEquals(ModerationStatus.VISIBLE, target.status().get());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ReportStatus.RESOLVED, result.status());
        verify(comments).restorePostComments(targetId);
        verifyAudit(AdminAction.CONTENT_RESTORED, ReportTargetType.POST);
    }

    @ParameterizedTest
    @EnumSource(value = ReportTargetType.class, names = {"COMMENT", "PROFILE"})
    void restoringNonPostContentDoesNotTouchPostComments(ReportTargetType type) {
        var report = stubPendingReport(type);
        var target = stubTarget(type);
        target.markInReview().run();

        service.restore(reportId, admin.getId());

        assertEquals(ModerationStatus.VISIBLE, target.status().get());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        verify(comments, never()).restorePostComments(any());
        verifyAudit(AdminAction.CONTENT_RESTORED, type);
    }

    @Test
    void restoreFailsWhenContentIsNotInReview() {
        stubPendingReport(ReportTargetType.POST);
        stubTarget(ReportTargetType.POST);

        assertThrows(ModerationStateConflictException.class, () -> service.restore(reportId, admin.getId()));
        verify(comments, never()).restorePostComments(any());
    }

    // ---------- remove ----------

    @Test
    void removingAPostRemovesItsCommentsAndResolvesRelatedReports() {
        var report = stubPendingReport(ReportTargetType.POST);
        var target = stubTarget(ReportTargetType.POST);

        var result = service.remove(reportId, admin.getId());

        assertEquals(ModerationStatus.REMOVED, target.status().get());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ModerationStatus.REMOVED, result.moderationStatus());
        verify(comments).removePostComments(targetId);
        verify(reports).resolvePendingCommentReportsForPost(targetId);
        verifyAudit(AdminAction.CONTENT_REMOVED, ReportTargetType.POST);
    }

    @ParameterizedTest
    @EnumSource(value = ReportTargetType.class, names = {"COMMENT", "PROFILE"})
    void removingNonPostContentDoesNotCascade(ReportTargetType type) {
        var report = stubPendingReport(type);
        var target = stubTarget(type);

        service.remove(reportId, admin.getId());

        assertEquals(ModerationStatus.REMOVED, target.status().get());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        verify(comments, never()).removePostComments(any());
        verify(reports, never()).resolvePendingCommentReportsForPost(any());
        verifyAudit(AdminAction.CONTENT_REMOVED, type);
    }

    @Test
    void removeFailsWhenContentIsAlreadyRemoved() {
        stubPendingReport(ReportTargetType.COMMENT);
        stubTarget(ReportTargetType.COMMENT).markRemoved().run();

        assertThrows(ModerationStateConflictException.class, () -> service.remove(reportId, admin.getId()));
    }

    // ---------- dismiss ----------

    @Test
    void dismissingClosesTheReportWithoutTouchingTheContent() {
        var report = stubPendingReport(ReportTargetType.POST);
        var target = stubTarget(ReportTargetType.POST);

        var result = service.dismiss(reportId, admin.getId());

        assertEquals(ReportStatus.DISMISSED, report.getStatus());
        assertEquals(ReportStatus.DISMISSED, result.status());
        assertEquals(ModerationStatus.VISIBLE, target.status().get());
        verifyAudit(AdminAction.REPORT_DISMISSED, ReportTargetType.POST);
    }

    // ---------- errores comunes ----------

    @Test
    void failsWhenTheReportDoesNotExist() {
        when(reports.findWithReporterById(reportId)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> service.review(reportId, admin.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = ReportStatus.class, names = {"RESOLVED", "DISMISSED"})
    void failsWhenTheReportIsNotPending(ReportStatus closed) {
        var report = report(ReportTargetType.POST);
        if (closed == ReportStatus.RESOLVED) report.resolve(); else report.dismiss();
        when(reports.findWithReporterById(reportId)).thenReturn(Optional.of(report));

        assertThrows(ModerationStateConflictException.class, () -> service.dismiss(reportId, admin.getId()));
        verify(users, never()).findById(any());
    }

    @Test
    void failsWhenTheAdminDoesNotExist() {
        when(reports.findWithReporterById(reportId)).thenReturn(Optional.of(report(ReportTargetType.POST)));
        when(users.findById(admin.getId())).thenReturn(Optional.empty());

        assertThrows(ModerationTargetNotFoundException.class, () -> service.remove(reportId, admin.getId()));
    }

    @ParameterizedTest
    @EnumSource(ReportTargetType.class)
    void failsWhenTheReportedTargetNoLongerExists(ReportTargetType type) {
        // Sin stubs de destino: los repositorios mockeados devuelven Optional.empty().
        stubPendingReport(type);

        assertThrows(ModerationTargetNotFoundException.class, () -> service.review(reportId, admin.getId()));
    }
}
