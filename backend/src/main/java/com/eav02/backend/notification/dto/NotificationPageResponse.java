package com.eav02.backend.notification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.eav02.backend.notification.entity.Notification;

public record NotificationPageResponse(List<NotificationResponse> content, int page, int size,
        long totalElements, int totalPages) {

    public static NotificationPageResponse from(Page<Notification> result) {
        return new NotificationPageResponse(result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}