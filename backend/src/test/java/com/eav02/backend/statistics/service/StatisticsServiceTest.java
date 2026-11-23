package com.eav02.backend.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eav02.backend.statistics.dto.StatisticsGroupBy;
import com.eav02.backend.statistics.exception.InvalidStatisticsRequestException;
import com.eav02.backend.statistics.repository.StatisticsAggregateRow;
import com.eav02.backend.statistics.repository.StatisticsRepository;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StatisticsRepository statistics;

    private StatisticsService service;

    @BeforeEach
    void setUp() {
        service = new StatisticsService(statistics);
    }

    @Test
    void groupsPartialMonthlyRangeAndSumsAllMetrics() {
        when(statistics.aggregate(
                Instant.parse("2025-12-31T05:00:00Z"),
                Instant.parse("2026-02-02T05:00:00Z"),
                StatisticsGroupBy.MONTH))
                .thenReturn(List.of(
                        new StatisticsAggregateRow(LocalDate.parse("2025-12-01"), 1, 0, 0, 1),
                        new StatisticsAggregateRow(LocalDate.parse("2026-01-01"), 2, 1, 3, 4),
                        new StatisticsAggregateRow(LocalDate.parse("2026-02-01"), 3, 2, 1, 0)));

        var result = service.getStatistics(
                LocalDate.parse("2025-12-31"), LocalDate.parse("2026-02-01"), StatisticsGroupBy.MONTH);

        assertEquals(LocalDate.parse("2025-12-31"), result.startDate());
        assertEquals(LocalDate.parse("2026-02-01"), result.endDate());
        assertEquals(StatisticsGroupBy.MONTH, result.groupBy());
        assertEquals(6, result.totals().users());
        assertEquals(3, result.totals().projects());
        assertEquals(4, result.totals().posts());
        assertEquals(5, result.totals().comments());
        assertEquals(List.of("2025-12-01", "2026-01-01", "2026-02-01"),
                result.series().stream().map(period -> period.period()).toList());
        assertEquals(1, result.series().get(0).users());
    }

    @Test
    void groupsWeeksFromMondayAcrossYearBoundaryAndUsesBogotaDateBoundaries() {
        when(statistics.aggregate(
                Instant.parse("2024-12-31T05:00:00Z"),
                Instant.parse("2025-01-09T05:00:00Z"),
                StatisticsGroupBy.WEEK))
                .thenReturn(List.of(
                        new StatisticsAggregateRow(LocalDate.parse("2024-12-30"), 4, 3, 2, 1),
                        new StatisticsAggregateRow(LocalDate.parse("2025-01-06"), 1, 0, 5, 2)));

        var result = service.getStatistics(
                LocalDate.parse("2024-12-31"), LocalDate.parse("2025-01-08"), StatisticsGroupBy.WEEK);

        assertEquals(List.of("2024-12-30", "2025-01-06"),
                result.series().stream().map(period -> period.period()).toList());
        assertEquals(5, result.totals().users());
        assertEquals(3, result.totals().projects());
        assertEquals(7, result.totals().posts());
        assertEquals(3, result.totals().comments());
    }

    @Test
    void includesEmptyMonthlyPeriodsAndReturnsZeroWhenThereAreNoEvents() {
        when(statistics.aggregate(
                Instant.parse("2026-04-29T05:00:00Z"),
                Instant.parse("2026-05-02T05:00:00Z"),
                StatisticsGroupBy.MONTH)).thenReturn(List.of());

        var result = service.getStatistics(
                LocalDate.parse("2026-04-29"), LocalDate.parse("2026-05-01"), StatisticsGroupBy.MONTH);

        assertEquals(List.of("2026-04-01", "2026-05-01"),
                result.series().stream().map(period -> period.period()).toList());
        assertEquals(0, result.totals().users());
        assertEquals(0, result.totals().projects());
        assertEquals(0, result.totals().posts());
        assertEquals(0, result.totals().comments());
        result.series().forEach(period -> {
            assertEquals(0, period.users());
            assertEquals(0, period.projects());
            assertEquals(0, period.posts());
            assertEquals(0, period.comments());
        });
    }

    @Test
    void rejectsInvertedDateRangeBeforeQuerying() {
        assertThrows(InvalidStatisticsRequestException.class, () -> service.getStatistics(
                LocalDate.parse("2026-05-02"), LocalDate.parse("2026-05-01"), StatisticsGroupBy.MONTH));

        verify(statistics, never()).aggregate(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsARangeOfExactlyFiveCalendarYears() {
        when(statistics.aggregate(
                Instant.parse("2021-01-01T05:00:00Z"),
                Instant.parse("2026-01-02T05:00:00Z"),
                StatisticsGroupBy.MONTH)).thenReturn(List.of());

        var result = service.getStatistics(
                LocalDate.parse("2021-01-01"), LocalDate.parse("2026-01-01"), StatisticsGroupBy.MONTH);

        assertEquals(61, result.series().size());
        verify(statistics).aggregate(
                Instant.parse("2021-01-01T05:00:00Z"),
                Instant.parse("2026-01-02T05:00:00Z"),
                StatisticsGroupBy.MONTH);
    }

    @Test
    void rejectsARangeOverFiveCalendarYearsBeforeQuerying() {
        assertThrows(InvalidStatisticsRequestException.class, () -> service.getStatistics(
                LocalDate.parse("2021-01-01"), LocalDate.parse("2026-01-02"), StatisticsGroupBy.MONTH));

        verify(statistics, never()).aggregate(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void passesDateBoundariesAsInstantsForInclusiveBogotaCalendarDates() {
        when(statistics.aggregate(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StatisticsGroupBy.MONTH)))
                .thenReturn(List.of());

        service.getStatistics(
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-01"), StatisticsGroupBy.MONTH);

        var start = ArgumentCaptor.forClass(Instant.class);
        var end = ArgumentCaptor.forClass(Instant.class);
        verify(statistics).aggregate(start.capture(), end.capture(),
                org.mockito.ArgumentMatchers.eq(StatisticsGroupBy.MONTH));
        assertEquals(Instant.parse("2026-01-01T05:00:00Z"), start.getValue());
        assertEquals(Instant.parse("2026-01-02T05:00:00Z"), end.getValue());
    }
}
