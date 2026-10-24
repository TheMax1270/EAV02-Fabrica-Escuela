package com.eav02.backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.service.AdminAuditService;
import com.eav02.backend.common.exception.SelfSuspensionException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.common.exception.UserStatusConflictException;
import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserRepository users;

    @Mock
    private AdminAuditService audit;

    private AdminUserService service;

    private AppUser admin;
    private AppUser target;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(users, audit);
        admin = user(true);
        target = user(true);
    }

    private AppUser user(boolean enabled) {
        var appUser = new AppUser("Grace Hopper", "grace", "grace@example.com", "hash",
                UserRole.DEVELOPER, enabled, NOW);
        ReflectionTestUtils.setField(appUser, "id", UUID.randomUUID());
        return appUser;
    }

    @Test
    void listsUsersFilteredByStatus() {
        Page<AppUser> page = new PageImpl<>(java.util.List.of(target));
        when(users.findAllByEnabled(eq(true), any(Pageable.class))).thenReturn(page);

        var response = service.list(AccountStatus.ACTIVE, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals(target.getUsername(), response.content().get(0).username());
    }

    @Test
    void listsAllUsersWhenStatusIsNull() {
        Page<AppUser> page = new PageImpl<>(java.util.List.of(admin, target));
        when(users.findAll(any(Pageable.class))).thenReturn(page);

        var response = service.list(null, 0, 20);

        assertEquals(2, response.totalElements());
    }

    @Test
    void suspendsAnEnabledUser() {
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(target.getId())).thenReturn(Optional.of(target));

        var response = service.suspend(admin.getId(), target.getId());

        assertEquals(AccountStatus.SUSPENDED, response.status());
        verify(audit).record(eq(admin), eq(AdminAction.USER_SUSPENDED), eq(AdminAuditService.TARGET_USER),
                eq(target.getId()), any());
    }

    @Test
    void adminCannotSuspendThemselves() {
        assertThrows(SelfSuspensionException.class, () -> service.suspend(admin.getId(), admin.getId()));
        verify(users, never()).findById(any());
    }

    @Test
    void suspendingAlreadySuspendedUserConflicts() {
        var suspended = user(false);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        assertThrows(UserStatusConflictException.class, () -> service.suspend(admin.getId(), suspended.getId()));
    }

    @Test
    void suspendingUnknownUserThrowsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.suspend(admin.getId(), missingId));
    }

    @Test
    void reactivatesASuspendedUser() {
        var suspended = user(false);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        var response = service.reactivate(admin.getId(), suspended.getId());

        assertEquals(AccountStatus.ACTIVE, response.status());
        verify(audit).record(eq(admin), eq(AdminAction.USER_REACTIVATED), eq(AdminAuditService.TARGET_USER),
                eq(suspended.getId()), any());
    }

    @Test
    void reactivatingAlreadyActiveUserConflicts() {
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(target.getId())).thenReturn(Optional.of(target));

        assertThrows(UserStatusConflictException.class, () -> service.reactivate(admin.getId(), target.getId()));
    }
}