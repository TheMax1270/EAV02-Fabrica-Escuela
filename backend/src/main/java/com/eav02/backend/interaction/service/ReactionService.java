package com.eav02.backend.interaction.service;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eav02.backend.common.exception.*;
import com.eav02.backend.interaction.dto.ReactionSummaryResponse;
import com.eav02.backend.interaction.repository.ReactionRepository;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class ReactionService {
    private final ReactionRepository reactions;
    private final ProjectRepository projects;
    private final PostRepository posts;
    private final UserRepository users;
    private final Clock clock;
    public ReactionService(ReactionRepository reactions, ProjectRepository projects, PostRepository posts, UserRepository users, Clock clock) {
        this.reactions = reactions; this.projects = projects; this.posts = posts; this.users = users; this.clock = clock;
    }
    private void target(UUID id, boolean project) {
        if (project && !projects.existsById(id)) throw new ProjectNotFoundException();
        if (!project && !posts.existsById(id)) throw new PostNotFoundException();
    }
    @Transactional
    public void like(UUID id, boolean project, UUID userId) {
        target(id, project);
        if (!users.existsById(userId)) throw new UserNotFoundException();
        reactions.insertLike(UUID.randomUUID(), userId, project ? id : null, project ? null : id, clock.instant());
    }
    @Transactional
    public void unlike(UUID id, boolean project, UUID userId) {
        target(id, project);
        if (project) reactions.deleteByProject_IdAndUser_Id(id, userId);
        else reactions.deleteByPost_IdAndUser_Id(id, userId);
    }
    @Transactional(readOnly = true)
    public ReactionSummaryResponse summary(UUID id, boolean project, UUID userId) {
        target(id, project);
        long count = project ? reactions.countByProject_Id(id) : reactions.countByPost_Id(id);
        Boolean liked = userId == null ? null : project ? reactions.existsByProject_IdAndUser_Id(id, userId)
                : reactions.existsByPost_IdAndUser_Id(id, userId);
        var usernames = project ? reactions.findProjectUsernames(id) : reactions.findPostUsernames(id);
        return new ReactionSummaryResponse(count, liked, usernames);
    }
}
