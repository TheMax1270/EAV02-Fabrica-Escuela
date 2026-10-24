package com.eav02.backend.moderation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.CannotReportOwnContentException;
import com.eav02.backend.common.exception.CommentNotFoundException;
import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.ProfileNotFoundException;
import com.eav02.backend.common.exception.ReportAlreadyExistsException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.moderation.dto.ReportRequest;
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
class ReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    @Mock private ReportRepository reports;
    @Mock private UserRepository users;
    @Mock private PostRepository posts;
    @Mock private CommentRepository comments;
    @Mock private DeveloperProfileRepository profiles;

    private ReportService service;
    private final UUID targetId = UUID.randomUUID();
    private AppUser author;
    private AppUser reporter;
    private final ReportRequest request = new ReportRequest(ReportReason.SPAM, "  contenido sospechoso ");

    @BeforeEach
    void setUp() {
        service = new ReportService(reports, users, posts, comments, profiles,
                Clock.fixed(NOW, ZoneOffset.UTC));
        author = user("autor");
        reporter = user("reportante");
    }

    private static AppUser user(String username) {
        var user = new AppUser("Nombre " + username, username, username + "@example.com", "hash",
                UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Post post() {
        return new Post(author, "Titulo", "Contenido", NOW);
    }

    private Comment comment() {
        return new Comment(author, null, post(), null, "Comentario", NOW);
    }

    private DeveloperProfile profile() {
        return new DeveloperProfile(author, "Bio", List.of("Java"), List.of("Spring"),
                ExperienceLevel.JUNIOR, null, null, null, NOW);
    }

    private void stubSuccessfulCreation() {
        when(users.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(reports.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(any(), any(), any(), any()))
                .thenReturn(false);
        when(reports.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reportsAPostAndStoresItAsPending() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post()));
        stubSuccessfulCreation();

        var response = service.reportPost(targetId, reporter.getId(), request);

        var captor = ArgumentCaptor.forClass(Report.class);
        verify(reports).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertEquals(ReportTargetType.POST, saved.getTargetType());
        assertEquals(targetId, saved.getTargetId());
        assertEquals(ReportStatus.PENDING, saved.getStatus());
        assertEquals(NOW, saved.getCreatedAt());
        assertEquals("contenido sospechoso", saved.getDetails());
        assertEquals(reporter.getId(), response.reporterId());
        assertEquals("reportante", response.reporterUsername());
        assertEquals(ReportReason.SPAM, response.reason());
        assertEquals(ReportStatus.PENDING, response.status());
    }

    @Test
    void reportsACommentAndStoresItAsPending() {
        when(comments.findWithTargetById(targetId)).thenReturn(Optional.of(comment()));
        stubSuccessfulCreation();

        var response = service.reportComment(targetId, reporter.getId(), request);

        assertEquals(ReportTargetType.COMMENT, response.targetType());
        assertEquals(targetId, response.targetId());
    }

    @Test
    void reportsAProfileAndStoresItAsPending() {
        when(profiles.findWithUserById(targetId)).thenReturn(Optional.of(profile()));
        stubSuccessfulCreation();

        var response = service.reportProfile(targetId, reporter.getId(), request);

        assertEquals(ReportTargetType.PROFILE, response.targetType());
        assertEquals(targetId, response.targetId());
    }

    @Test
    void acceptsReportsWithoutDetails() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post()));
        stubSuccessfulCreation();

        var response = service.reportPost(targetId, reporter.getId(), new ReportRequest(ReportReason.OTHER, "  "));

        assertNull(response.details());
    }

    @Test
    void rejectsReportingOwnPost() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post()));

        assertThrows(CannotReportOwnContentException.class,
                () -> service.reportPost(targetId, author.getId(), request));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void rejectsReportingOwnComment() {
        when(comments.findWithTargetById(targetId)).thenReturn(Optional.of(comment()));

        assertThrows(CannotReportOwnContentException.class,
                () -> service.reportComment(targetId, author.getId(), request));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void rejectsReportingOwnProfile() {
        when(profiles.findWithUserById(targetId)).thenReturn(Optional.of(profile()));

        assertThrows(CannotReportOwnContentException.class,
                () -> service.reportProfile(targetId, author.getId(), request));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void failsWhenThePostDoesNotExist() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> service.reportPost(targetId, reporter.getId(), request));
    }

    @Test
    void failsWhenTheCommentDoesNotExist() {
        when(comments.findWithTargetById(targetId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> service.reportComment(targetId, reporter.getId(), request));
    }

    @Test
    void failsWhenTheProfileDoesNotExist() {
        when(profiles.findWithUserById(targetId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> service.reportProfile(targetId, reporter.getId(), request));
    }

    @Test
    void failsWhenTheReporterDoesNotExist() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post()));
        when(users.findById(reporter.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.reportPost(targetId, reporter.getId(), request));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void rejectsADuplicatedPendingReport() {
        when(posts.findWithUserById(targetId)).thenReturn(Optional.of(post()));
        when(users.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(reports.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporter.getId(),
                ReportTargetType.POST, targetId, ReportStatus.PENDING)).thenReturn(true);

        assertThrows(ReportAlreadyExistsException.class,
                () -> service.reportPost(targetId, reporter.getId(), request));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void statusReflectsWhetherThereIsAPendingReport() {
        when(reports.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporter.getId(),
                ReportTargetType.COMMENT, targetId, ReportStatus.PENDING)).thenReturn(true);
        when(reports.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporter.getId(),
                ReportTargetType.PROFILE, targetId, ReportStatus.PENDING)).thenReturn(false);

        assertTrue(service.status(ReportTargetType.COMMENT, targetId, reporter.getId()).reported());
        assertFalse(service.status(ReportTargetType.PROFILE, targetId, reporter.getId()).reported());
    }
}
