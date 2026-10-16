package com.eav02.backend.moderation.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminReportPageResponse(List<AdminReportResponse> content, int page, int size,
        long totalElements, int totalPages) {

    public static AdminReportPageResponse from(Page<AdminReportResponse> reports) {
        return new AdminReportPageResponse(reports.getContent(), reports.getNumber(), reports.getSize(),
                reports.getTotalElements(), reports.getTotalPages());
    }
}
