package com.eav02.backend.statistics.repository;

import java.time.LocalDate;

public record StatisticsAggregateRow(
        LocalDate period,
        long users,
        long projects,
        long posts,
        long comments) {
}
