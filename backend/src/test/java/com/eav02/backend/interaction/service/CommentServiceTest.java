package com.eav02.backend.interaction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.eav02.backend.common.exception.*;
import com.eav02.backend.interaction.dto.*;
import com.eav02.backend.interaction.entity.Comment;
import com.eav02.backend.interaction.repository.CommentRepository;
import com.eav02.backend.project.entity.*;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.post.entity.*;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.entity.*;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    @Mock CommentRepository comments;
    @Mock ProjectRepository projects;
    @Mock PostRepository posts;
    @Mock UserRepository users;
    CommentService service;
    AppUser author;
    Project project;
    @BeforeEach void setup() {
        service = new CommentService(comments, projects, posts, users, Clock.fixed(NOW, ZoneOffset.UTC));
        author = new AppUser("Ada", "ada", "ada@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(author, "id", UUID.randomUUID());
        project = new Project(author, "Project", "Description", List.of("Java"), ProjectStatus.IN_PROGRESS, null, NOW);
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());
    }
    void writing() {
        when(users.findById(author.getId())).thenReturn(Optional.of(author));
        when(comments.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
            return comment;
        });
    }
    @Test void commentsOnProjectWithAuthorAndDate() {
        writing();
        when(projects.findById(project.getId())).thenReturn(Optional.of(project));
        var result = service.commentProject(project.getId(), author.getId(), new CommentRequest(" Texto\nsegunda linea "));
        assertEquals(project.getId(), result.projectId());
        assertNull(result.postId()); assertNull(result.parentCommentId());
        assertEquals(author.getId(), result.userId()); assertEquals("ada", result.username());
        assertEquals(NOW, result.createdAt()); assertEquals("Texto\nsegunda linea", result.content());
    }
    @ParameterizedTest @EnumSource(PostType.class)
    void commentsOnBothPostTypes(PostType type) {
        writing();
        var post = post(type);
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        var result = service.commentPost(post.getId(), author.getId(), new CommentRequest("Texto"));
        assertEquals(post.getId(), result.postId()); assertNull(result.projectId());
    }
    @Test void repliesToRootFromAnotherAuthorAndInheritsTarget() {
        var parent = root();
        var responder = new AppUser("Grace", "grace", "grace@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(responder, "id", UUID.randomUUID());
        when(users.findById(responder.getId())).thenReturn(Optional.of(responder));
        when(comments.findWithTargetById(parent.getId())).thenReturn(Optional.of(parent));
        when(comments.saveAndFlush(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.reply(parent.getId(), responder.getId(), new CommentRequest("Respuesta"));
        assertEquals(parent.getId(), result.parentCommentId()); assertEquals(project.getId(), result.projectId());
        assertEquals(responder.getId(), result.userId()); assertEquals(NOW, result.createdAt());
    }
    @Test void repliesToPostRoot() {
        writing();
        var post = post(PostType.GENERAL);
        var parent = new Comment(author, null, post, null, "Raiz", NOW);
        ReflectionTestUtils.setField(parent, "id", UUID.randomUUID());
        when(comments.findWithTargetById(parent.getId())).thenReturn(Optional.of(parent));
        var result = service.reply(parent.getId(), author.getId(), new CommentRequest("Respuesta"));
        assertEquals(post.getId(), result.postId()); assertNull(result.projectId());
    }
        @Test void rejectsReplyToReplyBeforeWriting() {
        var child = new Comment(author, project, null, root(), "Respuesta", NOW);
        ReflectionTestUtils.setField(child, "id", UUID.randomUUID());
        when(comments.findWithTargetById(child.getId())).thenReturn(Optional.of(child));
        assertThrows(ReplyDepthException.class, () -> service.reply(child.getId(), author.getId(), new CommentRequest("Otra")));
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(users);
        assertThrows(ReplyDepthException.class, () -> service.listReplies(child.getId(), 0, 20));
    }
    @Test void missingParentIs404DomainException() {
        UUID id = UUID.randomUUID();
        assertThrows(CommentNotFoundException.class, () -> service.reply(id, author.getId(), new CommentRequest("Texto")));
        verify(comments, never()).saveAndFlush(any());
    }
    @Test void missingProjectDoesNotWrite() {
        assertThrows(ProjectNotFoundException.class, () -> service.commentProject(project.getId(), author.getId(), new CommentRequest("Texto")));
        verifyNoInteractions(comments, users);
    }
    @Test void missingPostDoesNotWrite() {
        assertThrows(PostNotFoundException.class, () -> service.commentPost(UUID.randomUUID(), author.getId(), new CommentRequest("Texto")));
        verifyNoInteractions(comments, users);
    }
    @Test void paginatesRootsWithBatchedReplyCounts() {
        var root = root();
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
        when(projects.existsById(project.getId())).thenReturn(true);
        when(comments.findProjectRoots(project.getId(), pageable)).thenReturn(new PageImpl<>(List.of(root), pageable, 1));
        var count = mock(CommentRepository.ReplyCount.class);
        when(count.getParentId()).thenReturn(root.getId()); when(count.getTotal()).thenReturn(3L);
        when(comments.countReplies(List.of(root.getId()))).thenReturn(List.of(count));
        var result = service.listProject(project.getId(), 0, 20);
        assertEquals(3, result.content().getFirst().replyCount()); assertEquals(1, result.totalElements());
        verify(comments).countReplies(List.of(root.getId()));
    }
    @Test void paginatesPostRoots() {
        var post = post(PostType.GENERAL);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
        when(posts.existsById(post.getId())).thenReturn(true);
        when(comments.findPostRoots(post.getId(), pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertEquals(0, service.listPost(post.getId(), 0, 20).totalElements());
        verify(comments, never()).countReplies(any());
    }
    @Test void paginatesRepliesWithoutNestedCounts() {
        var root = root();
        var child = new Comment(author, project, null, root, "Respuesta", NOW);
        ReflectionTestUtils.setField(child, "id", UUID.randomUUID());
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
        when(comments.findWithTargetById(root.getId())).thenReturn(Optional.of(root));
        when(comments.findReplies(root.getId(), pageable)).thenReturn(new PageImpl<>(List.of(child), pageable, 1));
        assertEquals(0, service.listReplies(root.getId(), 0, 20).content().getFirst().replyCount());
        verify(comments, never()).countReplies(any());
    }
    @Test void modelRejectsMissingAndDoubleTarget() {
        var post = post(PostType.GENERAL);
        assertThrows(IllegalArgumentException.class, () -> new Comment(author, null, null, null, "Text", NOW));
        assertThrows(IllegalArgumentException.class, () -> new Comment(author, project, post, null, "Text", NOW));
    }
    @Test void modelRejectsParentFromDifferentTarget() {
        var other = new Project(author, "Other", "Desc", List.of("Java"), ProjectStatus.IN_PROGRESS, null, NOW);
        ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> new Comment(author, other, null, root(), "Text", NOW));
        assertThrows(IllegalArgumentException.class, () -> new Comment(author, null, post(PostType.GENERAL), root(), "Text", NOW));
    }
    private Comment root() {
        var root = new Comment(author, project, null, null, "Raiz", NOW);
        ReflectionTestUtils.setField(root, "id", UUID.randomUUID()); return root;
    }
    private Post post(PostType type) {
        var post = new Post(author, "Post", "Text", type, null, type == PostType.GENERAL ? List.of() : List.of("Java"), NOW);
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID()); return post;
    }
}
