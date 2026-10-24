package com.eav02.backend.user.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.admin.entity.AdminAction;
import com.eav02.backend.admin.service.AdminAuditService;
import com.eav02.backend.common.exception.SelfSuspensionException;
import com.eav02.backend.common.exception.UserNotFoundException;
import com.eav02.backend.common.exception.UserStatusConflictException;
import com.eav02.backend.user.dto.AdminUserResponse;
import com.eav02.backend.user.dto.UserPageResponse;
import com.eav02.backend.user.entity.AccountStatus;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class AdminUserService implements IAdminUserService {

    private final UserRepository users;
    private final AdminAuditService audit;

    public AdminUserService(UserRepository users, AdminAuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(AccountStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null
                ? users.findAll(pageable)
                : users.findAllByEnabled(status == AccountStatus.ACTIVE, pageable);
        return UserPageResponse.from(result);
    }

    @Transactional
    public AdminUserResponse suspend(UUID adminId, UUID userId) {
        if (adminId.equals(userId)) {
            throw new SelfSuspensionException();
        }
        AppUser admin = findUser(adminId);
        AppUser user = findUser(userId);
        if (!user.isEnabled()) {
            throw new UserStatusConflictException(user.getStatus());
        }
        user.suspend();
        users.flush();
        audit.record(admin, AdminAction.USER_SUSPENDED, AdminAuditService.TARGET_USER, userId,
                statusChange(AccountStatus.ACTIVE, AccountStatus.SUSPENDED));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse reactivate(UUID adminId, UUID userId) {
        AppUser admin = findUser(adminId);
        AppUser user = findUser(userId);
        if (user.isEnabled()) {
            throw new UserStatusConflictException(user.getStatus());
        }
        user.reactivate();
        users.flush();
        audit.record(admin, AdminAction.USER_REACTIVATED, AdminAuditService.TARGET_USER, userId,
                statusChange(AccountStatus.SUSPENDED, AccountStatus.ACTIVE));
        return AdminUserResponse.from(user);
    }

    private AppUser findUser(UUID id) {
        return users.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private static Map<String, Object> statusChange(AccountStatus previous, AccountStatus next) {
        return Map.of("previousStatus", previous.name(), "newStatus", next.name());
    }
}
