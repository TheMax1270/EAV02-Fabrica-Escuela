package com.eav02.backend.user.service;

import java.util.UUID;

import com.eav02.backend.user.dto.AdminUserResponse;
import com.eav02.backend.user.dto.UserPageResponse;
import com.eav02.backend.user.entity.AccountStatus;

public interface IAdminUserService {

    UserPageResponse list(AccountStatus status, int page, int size);

    AdminUserResponse suspend(UUID adminId, UUID userId);

    AdminUserResponse reactivate(UUID adminId, UUID userId);
}