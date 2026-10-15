package com.eav02.backend.interaction.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
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
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final ProjectRepository projects;
    private final PostRepository posts;
    private final UserRepository users;
    private final Clock clock;
    public CommentService(CommentRepository comments, ProjectRepository projects, PostRepository posts, UserRepository users, Clock clock) {
        this.comments = comments; this.projects = projects; this.posts = posts; this.users = users; this.clock = clock;
    }
    @Transactional
    public CommentResponse commentProject(UUID id, UUID userId, CommentRequest request) {
        var project = projects.findById(id).orElseThrow(ProjectNotFoundException::new);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        return CommentResponse.from(comments.saveAndFlush(new Comment(user, project, null, null, request.content(), clock.instant())), 0);
    }
    @Transactional
    public CommentResponse commentPost(UUID id, UUID userId, CommentRequest request) {
        var post = posts.findById(id).orElseThrow(PostNotFoundException::new);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        return CommentResponse.from(comments.saveAndFlush(new Comment(user, null, post, null, request.content(), clock.instant())), 0);
    }
    @Transactional
    public CommentResponse reply(UUID id, UUID userId, CommentRequest request) {
        var parent = root(id);
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        return CommentResponse.from(comments.saveAndFlush(new Comment(user, parent.getProject(), parent.getPost(), parent, request.content(), clock.instant())), 0);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listProject(UUID id, int page, int size) {
        if (!projects.existsById(id)) throw new ProjectNotFoundException();
        return response(comments.findProjectRoots(id, pageable(page, size)), true);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listPost(UUID id, int page, int size) {
        if (!posts.existsById(id)) throw new PostNotFoundException();
        return response(comments.findPostRoots(id, pageable(page, size)), true);
    }
    @Transactional(readOnly = true)
    public CommentPageResponse listReplies(UUID id, int page, int size) {
        root(id);
        return response(comments.findReplies(id, pageable(page, size)), false);
    }
    private Comment root(UUID id) {
        var parent = comments.findWithTargetById(id).orElseThrow(CommentNotFoundException::new);
        if (parent.getParent() != null) throw new ReplyDepthException();
        return parent;
    }
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
    }
    private CommentPageResponse response(Page<Comment> result, boolean roots) {
        List<UUID> ids = result.getContent().stream().map(Comment::getId).toList();
        Map<UUID, Long> counts = roots && !ids.isEmpty() ? comments.countReplies(ids).stream()
                .collect(Collectors.toMap(CommentRepository.ReplyCount::getParentId, CommentRepository.ReplyCount::getTotal)) : Map.of();
        return new CommentPageResponse(result.getContent().stream().map(c -> CommentResponse.from(c, counts.getOrDefault(c.getId(), 0L))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
