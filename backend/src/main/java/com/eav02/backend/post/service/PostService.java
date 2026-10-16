package com.eav02.backend.post.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.PostOwnershipException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.post.dto.PostPageResponse;
import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.dto.PostResponse;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.moderation.entity.ModerationStatus;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.repository.UserRepository;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.interaction.repository.ReactionRepository;

@Service
public class PostService {

    private final PostRepository posts;
    private final UserRepository users;
    private final Clock clock;
    private final CommentRepository comments;
    private final ReactionRepository reactions;

    public PostService(PostRepository posts, UserRepository users, Clock clock,
            CommentRepository comments, ReactionRepository reactions) {
        this.posts = posts;
        this.users = users;
        this.clock = clock;
        this.comments = comments;
        this.reactions = reactions;
    }

    @Transactional
    public PostResponse create(UUID userId, PostRequest request) {
        var user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        var post = new Post(user, request.title(), request.content(), request.type(), request.link(),
                request.categories(), clock.instant());
        return PostResponse.from(posts.saveAndFlush(post));
    }

    @Transactional(readOnly = true)
    public PostResponse getById(UUID postId) {
        var post = posts.findWithUserById(postId).orElseThrow(PostNotFoundException::new);
        if (post.getModerationStatus() != ModerationStatus.VISIBLE) {
            throw new PostNotFoundException();
        }
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse resolve(UUID postId, UUID userId) {
        var post = posts.findWithUserById(postId).orElseThrow(PostNotFoundException::new);
        if (!post.getUser().getId().equals(userId)) throw new PostOwnershipException();
        post.resolve();
        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PostPageResponse list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PostPageResponse.from(posts.findAllWithUser(pageable));
    }

    @Transactional
    public void delete(UUID postId, UUID userId) {
        var post = posts.findForDeletion(postId).orElseThrow(PostNotFoundException::new);
        if (!post.getUser().getId().equals(userId)) throw new PostOwnershipException();
        reactions.deletePostReactions(postId);
        comments.deletePostReplies(postId);
        comments.deletePostRoots(postId);
        posts.delete(post);
        posts.flush();
    }
}
