package com.eav02.backend.interaction.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eav02.backend.common.exception.*;
import com.eav02.backend.interaction.dto.*;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.notification.service.NotificationService;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class CommentService {
    private final NotificationService notifications;
    private final CommentRepository comments;
    private final ProjectRepository projects;
    private final PostRepository posts;
    private final UserRepository users;
    private final Clock clock;
    public CommentService(CommentRepository comments, ProjectRepository projects, PostRepository posts,
        UserRepository users, Clock clock, NotificationService notifications) {
    this.comments = comments; this.projects = projects; this.posts = posts; this.users = users;
    this.clock = clock; this.notifications = notifications;
}
    @Transactional
    public CommentResponse commentProject(UUID id, UUID userId, CommentRequest request) {
        var project = projects.findById(id).orElseThrow(ProjectNotFoundException::new);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var saved = comments.saveAndFlush(new Comment(user, project, null, null, request.content(), clock.instant()));
        notifications.commentOnProject(project, userId);
        return CommentResponse.from(saved, 0);
    }
    @Transactional
    public CommentResponse commentPost(UUID id, UUID userId, CommentRequest request) {
        var post = posts.findById(id).orElseThrow(PostNotFoundException::new);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var saved = comments.saveAndFlush(new Comment(user, null, post, null, request.content(), clock.instant()));
        notifications.commentOnPost(post, userId);
        return CommentResponse.from(saved, 0);
    }
    @Transactional
    public CommentResponse reply(UUID id, UUID userId, CommentRequest request) {
        var parent = root(id);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var project = parent.getProject();
        var post = parent.getPost();
        var saved = comments.saveAndFlush(new Comment(user, project, post, parent, request.content(), clock.instant()));
        if (project != null) {
            notifications.commentOnProject(project, userId);
        } else {
            notifications.commentOnPost(Objects.requireNonNull(post), userId);
        }
        return CommentResponse.from(saved, 0);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listProject(UUID id, int page, int size) {
        if (!projects.existsById(id)) throw new ProjectNotFoundException();
        return response(comments.findProjectRoots(id, pageable(page, size)), true);
    }

    @Transactional
    public void removeOwn(UUID commentId, UUID userId) {
        var comment = comments.findWithTargetById(commentId).orElseThrow(CommentNotFoundException::new);
        if (!comment.getUser().getId().equals(userId)) throw new CommentOwnershipException();
        comments.removeCommentTree(commentId);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listPost(UUID id, int page, int size) {
        if (!posts.existsById(id)) throw new PostNotFoundException();
        return response(comments.findPostRoots(id, pageable(page, size)), true);
    }

    @Transactional(readOnly = true)
    public long countPost(UUID id) {
        if (!posts.existsById(id)) throw new PostNotFoundException();
        return comments.countVisiblePostComments(id);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listReplies(UUID id, int page, int size) {
        root(id);
        return response(comments.findReplies(id, pageable(page, size)), false);
    }
    private Comment root(UUID id) {
        return comments.findWithTargetById(id).orElseThrow(CommentNotFoundException::new);
    }
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
    }
    private CommentPageResponse response(Page<Comment> result, boolean roots) {
        List<UUID> ids = result.getContent().stream().map(Comment::getId).toList();
        Map<UUID, Long> counts = !ids.isEmpty() ? comments.countReplies(ids).stream()
                .collect(Collectors.toMap(CommentRepository.ReplyCount::getParentId, CommentRepository.ReplyCount::getTotal)) : Map.of();
        return new CommentPageResponse(result.getContent().stream().map(c -> CommentResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    
}
