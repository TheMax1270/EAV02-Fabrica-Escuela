package com.eav02.backend.statistics.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.statistics.dto.StatisticsGroupBy;
import com.eav02.backend.statistics.dto.StatisticsPeriod;
import com.eav02.backend.statistics.dto.StatisticsResponse;
import com.eav02.backend.statistics.dto.StatisticsTotals;
import com.eav02.backend.statistics.exception.InvalidStatisticsRequestException;
import com.eav02.backend.statistics.repository.StatisticsAggregateRow;
import com.eav02.backend.statistics.repository.StatisticsRepository;

@Service
public class StatisticsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StatisticsRepository statistics;

    public StatisticsService(StatisticsRepository statistics) {
        this.statistics = statistics;
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(
            LocalDate startDate, LocalDate endDate, StatisticsGroupBy groupBy) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidStatisticsRequestException("startDate", "No puede ser posterior a endDate");
        }
        if (endDate.equals(LocalDate.MAX)) {
            throw new InvalidStatisticsRequestException("endDate", "La fecha está fuera del rango permitido");
        }
        if (endDate.isAfter(startDate.plusYears(5))) {
            throw new InvalidStatisticsRequestException("endDate", "El rango no puede superar cinco años");
        }

        var startInclusive = startDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        var endExclusive = endDate.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        var rowsByPeriod = new HashMap<LocalDate, StatisticsAggregateRow>();
        statistics.aggregate(startInclusive, endExclusive, groupBy)
                .forEach(row -> rowsByPeriod.put(row.period(), row));

        var firstPeriod = firstPeriod(startDate, groupBy);
        var lastPeriod = firstPeriod(endDate, groupBy);
        var series = new ArrayList<StatisticsPeriod>();
        long totalUsers = 0;
        long totalProjects = 0;
        long totalPosts = 0;
        long totalComments = 0;

        for (var period = firstPeriod; !period.isAfter(lastPeriod); period = nextPeriod(period, groupBy)) {
            var row = rowsByPeriod.get(period);
            long users = row == null ? 0 : row.users();
            long projects = row == null ? 0 : row.projects();
            long posts = row == null ? 0 : row.posts();
            long comments = row == null ? 0 : row.comments();
            series.add(new StatisticsPeriod(PERIOD_FORMAT.format(period), users, projects, posts, comments));
            totalUsers = Math.addExact(totalUsers, users);
            totalProjects = Math.addExact(totalProjects, projects);
            totalPosts = Math.addExact(totalPosts, posts);
            totalComments = Math.addExact(totalComments, comments);
        }

        return new StatisticsResponse(startDate, endDate, groupBy,
                new StatisticsTotals(totalUsers, totalProjects, totalPosts, totalComments),
                List.copyOf(series));
    }

    private LocalDate firstPeriod(LocalDate date, StatisticsGroupBy groupBy) {
        return switch (groupBy) {
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextPeriod(LocalDate date, StatisticsGroupBy groupBy) {
        return switch (groupBy) {
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }
}
