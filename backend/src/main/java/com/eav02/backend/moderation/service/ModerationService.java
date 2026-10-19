package com.eav02.backend.moderation.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.service.AdminAuditService;
import com.eav02.backend.common.exception.ModerationStateConflictException;
import com.eav02.backend.common.exception.ModerationTargetNotFoundException;
import com.eav02.backend.common.exception.ReportNotFoundException;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.moderation.dto.AdminReportPageResponse;
import com.eav02.backend.moderation.dto.AdminReportResponse;
import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.repository.ReportRepository;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.profile.entity.DeveloperProfile;
import com.eav02.backend.profile.repository.DeveloperProfileRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class ModerationService {

    private enum Action { REVIEW, RESTORE, REMOVE, DISMISS }

    private final ReportRepository reports;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final DeveloperProfileRepository profiles;
    private final UserRepository users;
    private final AdminAuditService audit;

    public ModerationService(ReportRepository reports, PostRepository posts, CommentRepository comments,
            DeveloperProfileRepository profiles, UserRepository users, AdminAuditService audit) {
        this.reports = reports;
        this.posts = posts;
        this.comments = comments;
        this.profiles = profiles;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminReportPageResponse pending(int page, int size) {
        return list(ReportStatus.PENDING, page, size);
    }

    @Transactional(readOnly = true)
    public AdminReportPageResponse resolved(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<AdminReportResponse> result = reports.findByStatusesWithReporter(
                java.util.List.of(ReportStatus.RESOLVED, ReportStatus.DISMISSED), pageable).map(this::response);
        return AdminReportPageResponse.from(result);
    }

    private AdminReportPageResponse list(ReportStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<AdminReportResponse> result = reports.findByStatusWithReporter(status, pageable)
                .map(this::response);
        return AdminReportPageResponse.from(result);
    }

    @Transactional
    public AdminReportResponse review(UUID reportId, UUID adminId) {
        return act(reportId, adminId, Action.REVIEW);
    }

    @Transactional
    public AdminReportResponse restore(UUID reportId, UUID adminId) {
        return act(reportId, adminId, Action.RESTORE);
    }

    @Transactional
    public AdminReportResponse remove(UUID reportId, UUID adminId) {
        return act(reportId, adminId, Action.REMOVE);
    }

    @Transactional
    public AdminReportResponse dismiss(UUID reportId, UUID adminId) {
        return act(reportId, adminId, Action.DISMISS);
    }

    private AdminReportResponse act(UUID reportId, UUID adminId, Action action) {
        var report = reports.findWithReporterById(reportId).orElseThrow(ReportNotFoundException::new);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ModerationStateConflictException();
        }
        var admin = users.findById(adminId).orElseThrow(ModerationTargetNotFoundException::new);
        Target target = target(report);

        switch (action) {
            case REVIEW -> applyReview(report, target, admin);
            case RESTORE -> applyRestore(report, target, admin);
            case REMOVE -> applyRemove(report, target, admin);
            case DISMISS -> applyDismiss(report, admin);
        }
        reports.flush();
        return response(report);
    }

    private void applyReview(Report report, Target target, AppUser admin) {
        if (target.status() != ModerationStatus.VISIBLE) throw new ModerationStateConflictException();
        target.markInReview();
        recordAudit(admin, AdminAction.CONTENT_IN_REVIEW, report);
    }

    private void applyRestore(Report report, Target target, AppUser admin) {
        if (target.status() != ModerationStatus.IN_REVIEW) throw new ModerationStateConflictException();
        target.restore();
        if (report.getTargetType() == ReportTargetType.POST) {
            comments.restorePostComments(report.getTargetId());
        }
        report.resolve();
        recordAudit(admin, AdminAction.CONTENT_RESTORED, report);
    }

    private void applyRemove(Report report, Target target, AppUser admin) {
        if (target.status() == ModerationStatus.REMOVED) throw new ModerationStateConflictException();
        target.remove();
        if (report.getTargetType() == ReportTargetType.POST) {
            comments.removePostComments(report.getTargetId());
            reports.resolvePendingCommentReportsForPost(report.getTargetId());
        }
        report.resolve();
        recordAudit(admin, AdminAction.CONTENT_REMOVED, report);
    }

    private void applyDismiss(Report report, AppUser admin) {
        report.dismiss();
        recordAudit(admin, AdminAction.REPORT_DISMISSED, report);
    }

    private void recordAudit(AppUser admin, AdminAction action, Report report) {
        audit.record(admin, action, report.getTargetType().name(), report.getTargetId(),
                Map.of("reportId", report.getId().toString()));
    }

    private Target target(Report report) {
        return switch (report.getTargetType()) {
            case POST -> new PostTarget(posts.findWithUserById(report.getTargetId())
                    .orElseThrow(ModerationTargetNotFoundException::new));
            case COMMENT -> new CommentTarget(comments.findWithTargetById(report.getTargetId())
                    .orElseThrow(ModerationTargetNotFoundException::new));
            case PROFILE -> new ProfileTarget(profiles.findWithUserById(report.getTargetId())
                    .orElseThrow(ModerationTargetNotFoundException::new));
        };
    }

    private AdminReportResponse response(Report report) {
        Target target;
        try {
            target = target(report);
        } catch (ModerationTargetNotFoundException missing) {
            return new AdminReportResponse(report.getId(), report.getReporter().getId(),
                    report.getReporter().getUsername(), report.getTargetType(), report.getTargetId(),
                    "El elemento reportado ya no existe", null, report.getReason(), report.getDetails(),
                    report.getStatus(), null, report.getCreatedAt());
        }
        return new AdminReportResponse(report.getId(), report.getReporter().getId(),
                report.getReporter().getUsername(), report.getTargetType(), report.getTargetId(),
                target.preview(), target.username(), report.getReason(), report.getDetails(), report.getStatus(),
                target.status(), report.getCreatedAt());
    }

    private interface Target {
        ModerationStatus status();
        String preview();
        String username();
        void markInReview();
        void restore();
        void remove();
    }

    private record PostTarget(Post value) implements Target {
        public ModerationStatus status() { return value.getModerationStatus(); }
        public String preview() { return value.getTitle() == null ? value.getContent() : value.getTitle(); }
        public String username() { return value.getUser().getUsername(); }
        public void markInReview() { value.markInReview(); }
        public void restore() { value.restore(); }
        public void remove() { value.remove(); }
    }

    private record CommentTarget(Comment value) implements Target {
        public ModerationStatus status() { return value.getModerationStatus(); }
        public String preview() { return value.getContent(); }
        public String username() { return value.getUser().getUsername(); }
        public void markInReview() { value.markInReview(); }
        public void restore() { value.restore(); }
        public void remove() { value.remove(); }
    }

    private record ProfileTarget(DeveloperProfile value) implements Target {
        public ModerationStatus status() { return value.getModerationStatus(); }
        public String preview() { return value.getBiography(); }
        public String username() { return value.getUser().getUsername(); }
        public void markInReview() { value.markInReview(); }
        public void restore() { value.restore(); }
        public void remove() { value.remove(); }
    }
}
