package com.eav02.backend.moderation.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.CommentNotFoundException;
import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.ProfileNotFoundException;
import com.eav02.backend.common.exception.ReportAlreadyExistsException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.moderation.dto.ReportRequest;
import com.eav02.backend.moderation.dto.ReportResponse;
import com.eav02.backend.moderation.entity.Report;
import com.eav02.backend.moderation.entity.ReportStatus;
import com.eav02.backend.moderation.entity.ReportTargetType;
import com.eav02.backend.moderation.repository.ReportRepository;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.profile.repository.DeveloperProfileRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final UserRepository users;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final DeveloperProfileRepository profiles;
    private final Clock clock;

    public ReportService(ReportRepository reports, UserRepository users, PostRepository posts,
            CommentRepository comments, DeveloperProfileRepository profiles, Clock clock) {
        this.reports = reports;
        this.users = users;
        this.posts = posts;
        this.comments = comments;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Transactional
    public ReportResponse reportPost(UUID postId, UUID reporterId, ReportRequest request) {
        if (!posts.existsById(postId)) throw new PostNotFoundException();
        return create(ReportTargetType.POST, postId, reporterId, request);
    }

    @Transactional
    public ReportResponse reportComment(UUID commentId, UUID reporterId, ReportRequest request) {
        if (!comments.existsById(commentId)) throw new CommentNotFoundException();
        return create(ReportTargetType.COMMENT, commentId, reporterId, request);
    }

    @Transactional
    public ReportResponse reportProfile(UUID profileId, UUID reporterId, ReportRequest request) {
        if (!profiles.existsById(profileId)) throw new ProfileNotFoundException();
        return create(ReportTargetType.PROFILE, profileId, reporterId, request);
    }

    private ReportResponse create(ReportTargetType targetType, UUID targetId, UUID reporterId,
            ReportRequest request) {
        var reporter = users.findById(reporterId).orElseThrow(UserNotFoundException::new);
        if (reports.existsByReporter_IdAndTargetTypeAndTargetIdAndStatus(reporterId, targetType, targetId,
                ReportStatus.PENDING)) {
            throw new ReportAlreadyExistsException();
        }
        var report = new Report(reporter, targetType, targetId, request.reason(), request.details(),
                clock.instant());
        return ReportResponse.from(reports.saveAndFlush(report));
    }
}