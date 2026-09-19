package com.eav02.backend.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.eav02.backend.user.entity.AppUser;

public record UserPageResponse(List<AdminUserResponse> content, int page, int size, long totalElements,
        int totalPages) {

    public static UserPageResponse from(Page<AppUser> users) {
        return new UserPageResponse(users.getContent().stream().map(AdminUserResponse::from).toList(),
                users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages());
    }
}
