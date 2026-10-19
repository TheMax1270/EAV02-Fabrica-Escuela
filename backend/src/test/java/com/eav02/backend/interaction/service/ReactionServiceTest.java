package com.eav02.backend.interaction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.eav02.backend.interaction.repository.ReactionRepository;
import com.eav02.backend.project.repository.ProjectRepository;
import com.eav02.backend.post.repository.PostRepository;
import com.eav02.backend.user.repository.UserRepository;
import com.eav02.backend.common.exception.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {
    @Mock ReactionRepository reactions;
    @Mock ProjectRepository projects;
    @Mock PostRepository posts;
    @Mock UserRepository users;
    ReactionService service;
    UUID target = UUID.randomUUID(), user = UUID.randomUUID();
    Instant now = Instant.parse("2026-10-14T12:00:00Z");
    @BeforeEach void setup() { service = new ReactionService(reactions, projects, posts, users, Clock.fixed(now, ZoneOffset.UTC)); }
    void exists(boolean project) {
        if (project) when(projects.existsById(target)).thenReturn(true);
        else when(posts.existsById(target)).thenReturn(true);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void addsLikeAndRepeatedInsertConflictIsIdempotent(boolean project) {
        exists(project); when(users.existsById(user)).thenReturn(true);
        when(reactions.insertLike(any(), eq(user), eq(project ? target : null), eq(project ? null : target), eq(now))).thenReturn(1, 0);
        assertDoesNotThrow(() -> service.like(target, project, user));
        assertDoesNotThrow(() -> service.like(target, project, user));
        verify(reactions, times(2)).insertLike(any(), eq(user), eq(project ? target : null), eq(project ? null : target), eq(now));
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void removesOnlyOwnLikeIncludingMissingAssociation(boolean project) {
        exists(project);
        assertDoesNotThrow(() -> service.unlike(target, project, user));
        assertDoesNotThrow(() -> service.unlike(target, project, user));
        if (project) verify(reactions, times(2)).deleteByProject_IdAndUser_Id(target, user);
        else verify(reactions, times(2)).deleteByPost_IdAndUser_Id(target, user);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void returnsPublicCountWithoutPersonalState(boolean project) {
        exists(project);
        if (project) when(reactions.countByProject_Id(target)).thenReturn(4L);
        else when(reactions.countByPost_Id(target)).thenReturn(4L);
        var result = service.summary(target, project, null);
        assertEquals(4, result.likeCount()); assertNull(result.likedByMe());
        verify(reactions, never()).existsByProject_IdAndUser_Id(any(), any());
        verify(reactions, never()).existsByPost_IdAndUser_Id(any(), any());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void returnsAuthenticatedState(boolean project) {
        exists(project);
        if (project) when(reactions.existsByProject_IdAndUser_Id(target, user)).thenReturn(true);
        else when(reactions.existsByPost_IdAndUser_Id(target, user)).thenReturn(true);
        assertEquals(true, service.summary(target, project, user).likedByMe());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rejectsMissingTarget(boolean project) {
        Class<? extends RuntimeException> expected = project ? ProjectNotFoundException.class : PostNotFoundException.class;
        assertThrows(expected, () -> service.like(target, project, user));
        assertThrows(expected, () -> service.unlike(target, project, user));
        verifyNoInteractions(reactions, users);
    }
}
