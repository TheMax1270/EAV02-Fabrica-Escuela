package com.eav02.backend.post.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.PostNotFoundException;
import com.eav02.backend.common.exception.PostOwnershipException;
import com.eav02.backend.common.exception.PostTypeConflictException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.post.entity.PostType;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    @Mock private PostRepository posts;
    @Mock private UserRepository users;
    @Mock private com.eav02.backend.interaction.repository.CommentRepository comments;
    @Mock private com.eav02.backend.interaction.repository.ReactionRepository reactions;
    private PostService service;

    @BeforeEach
    void setUp() {
        service = new PostService(posts, users, Clock.fixed(NOW, ZoneOffset.UTC), comments, reactions);
    }

    @Test
    void createsPostWithResolvedAuthorAndClockPreservingInternalText() {
        var user = user();
        UUID postId = UUID.randomUUID();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(posts.saveAndFlush(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", postId);
            return post;
        });

        var response = service.create(user.getId(), new PostRequest("  Java  ",
                "  Primera linea\n\n<b>Texto</b> y **Markdown**  "));

        var captor = ArgumentCaptor.forClass(Post.class);
        verify(posts).saveAndFlush(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertEquals(postId, response.id());
        assertEquals(user.getId(), response.userId());
        assertEquals("ada", response.username());
        assertEquals("Java", response.title());
        assertEquals("Primera linea\n\n<b>Texto</b> y **Markdown**", response.content());
        assertEquals(NOW, response.createdAt());
    }

    @Test
    void missingAuthorDoesNotWritePost() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.create(userId, new PostRequest("Java", "Contenido")));
        verifyNoInteractions(posts);
    }

    @Test
    void getsPostWithPublicAuthorData() {
        var post = post();
        when(posts.findWithUserById(post.getId())).thenReturn(Optional.of(post));
        var response = service.getById(post.getId());
        assertEquals(post.getId(), response.id());
        assertEquals(post.getUser().getId(), response.userId());
        assertEquals("ada", response.username());
        assertEquals("Java", response.title());
        assertEquals("Contenido", response.content());
        assertEquals(NOW, response.createdAt());
    }

    @Test
    void missingPostThrowsDomainException() {
        UUID id = UUID.randomUUID();
        when(posts.findWithUserById(id)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void listsRequestedPageWithStableDescendingSortAndMetadata() {
        var post = post();
        var pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(posts.findAllWithUser(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 3));

        var response = service.list(1, 2);

        verify(posts).findAllWithUser(pageable);
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(3, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals(post.getId(), response.content().getFirst().id());
    }

    @Test
    void emptyListHasZeroTotals() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(posts.findAllWithUser(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        var response = service.list(0, 20);
        assertEquals(List.of(), response.content());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }

    private AppUser user() {
        var user = new AppUser("Ada", "ada", "ada@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3})
    void deletesOwnPostCleaningDependenciesInOrder(int interactions) {
        var post = post();
        UUID id = post.getId();
        when(posts.findForDeletion(id)).thenReturn(Optional.of(post));
        when(reactions.deletePostReactions(id)).thenReturn(interactions > 0 ? 2 : 0);
        when(comments.deletePostReplies(id)).thenReturn(interactions > 2 ? 3 : 0);
        when(comments.deletePostRoots(id)).thenReturn(interactions > 1 ? 1 : 0);
        service.delete(id, post.getUser().getId());
        var order = org.mockito.Mockito.inOrder(posts, reactions, comments);
        order.verify(posts).findForDeletion(id);
        order.verify(reactions).deletePostReactions(id);
        order.verify(comments).deletePostReplies(id);
        order.verify(comments).deletePostRoots(id);
        order.verify(posts).delete(post);
        order.verify(posts).flush();
        org.mockito.Mockito.verifyNoMoreInteractions(posts, reactions, comments);
    }

    @Test
    void cannotDeleteAnotherAuthorsPost() {
        var post = post();
        when(posts.findForDeletion(post.getId())).thenReturn(Optional.of(post));
        assertThrows(PostOwnershipException.class, () -> service.delete(post.getId(), UUID.randomUUID()));
        verifyNoInteractions(comments, reactions);
        org.mockito.Mockito.verify(posts, org.mockito.Mockito.never()).delete(any(Post.class));
    }

    @Test
    void deletingMissingPostDoesNotTouchInteractions() {
        UUID id = UUID.randomUUID();
        when(posts.findForDeletion(id)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class, () -> service.delete(id, UUID.randomUUID()));
        verifyNoInteractions(comments, reactions);
    }

    @Test
    void cleanupFailureStopsBeforeDeletingRootsAndPost() {
        var post = post();
        when(posts.findForDeletion(post.getId())).thenReturn(Optional.of(post));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("constraint"))
                .when(comments).deletePostReplies(post.getId());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.delete(post.getId(), post.getUser().getId()));
        org.mockito.Mockito.verify(comments, org.mockito.Mockito.never()).deletePostRoots(any());
        org.mockito.Mockito.verify(posts, org.mockito.Mockito.never()).delete(any(Post.class));
    }

    @Test
    void createsGeneralWithoutTitleWithOptionalLink() {
        var user = user();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(posts.saveAndFlush(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(user.getId(), new PostRequest(null, " Lanzamiento ",
                PostType.GENERAL, " https://example.com/release ", null));
        assertEquals(PostType.GENERAL, response.type());
        assertEquals(null, response.title());
        assertEquals("https://example.com/release", response.link());
        assertEquals(List.of(), response.categories());
        assertEquals(false, response.resolved());
    }

    @Test
    void createsTechnicalDiscussionWithNormalizedCategories() {
        var user = user();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(posts.saveAndFlush(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(user.getId(), new PostRequest(" Spring ", "Linea 1\nLinea 2",
                PostType.TECHNICAL_DISCUSSION, null, List.of(" Java ", "Spring Security")));
        assertEquals(PostType.TECHNICAL_DISCUSSION, response.type());
        assertEquals(List.of("Java", "Spring Security"), response.categories());
        assertEquals("Spring", response.title());
        assertEquals(false, response.resolved());
    }

    @Test
    void resolvesOwnDiscussionIdempotently() {
        var post = discussion();
        when(posts.findWithUserById(post.getId())).thenReturn(Optional.of(post));
        assertEquals(true, service.resolve(post.getId(), post.getUser().getId()).resolved());
        assertEquals(true, service.resolve(post.getId(), post.getUser().getId()).resolved());
        assertEquals(NOW, post.getCreatedAt());
    }

    @Test
    void cannotResolveAnotherAuthorsDiscussion() {
        var post = discussion();
        when(posts.findWithUserById(post.getId())).thenReturn(Optional.of(post));
        assertThrows(PostOwnershipException.class, () -> service.resolve(post.getId(), UUID.randomUUID()));
        assertEquals(false, post.isResolved());
    }

    @Test
    void cannotResolveGeneralPost() {
        var post = post();
        when(posts.findWithUserById(post.getId())).thenReturn(Optional.of(post));
        assertThrows(PostTypeConflictException.class, () -> service.resolve(post.getId(), post.getUser().getId()));
        assertEquals(false, post.isResolved());
    }

    @Test
    void resolvingMissingPostThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(posts.findWithUserById(id)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class, () -> service.resolve(id, UUID.randomUUID()));
    }

    @Test
    void listsAndGetsBothTypesIncludingResolvedState() {
        var general = post();
        var technical = discussion();
        technical.resolve();
        var pageable = PageRequest.of(0, 20);
        when(posts.findAllWithUser(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(general, technical), pageable, 2));
        when(posts.findWithUserById(technical.getId())).thenReturn(Optional.of(technical));
        var page = service.list(0, 20);
        assertEquals(PostType.GENERAL, page.content().get(0).type());
        assertEquals(PostType.TECHNICAL_DISCUSSION, page.content().get(1).type());
        assertEquals(List.of("Java"), service.getById(technical.getId()).categories());
        assertEquals(true, service.getById(technical.getId()).resolved());
    }

    private Post discussion() {
        var post = new Post(user(), "Spring", "Contenido", PostType.TECHNICAL_DISCUSSION,
                null, List.of("Java"), NOW);
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        return post;
    }

    private Post post() {
        var post = new Post(user(), "Java", "Contenido", NOW);
        ReflectionTestUtils.setField(post, "id", UUID.randomUUID());
        return post;
    }
}
