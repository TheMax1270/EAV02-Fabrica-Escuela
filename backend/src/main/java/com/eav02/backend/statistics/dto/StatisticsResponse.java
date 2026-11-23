package com.eav02.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        StatisticsGroupBy groupBy,
        StatisticsTotals totals,
        List<StatisticsPeriod> series) {
}
