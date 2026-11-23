package com.eav02.backend.statistics.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eav02.backend.statistics.dto.StatisticsGroupBy;
import com.eav02.backend.statistics.dto.StatisticsResponse;
import com.eav02.backend.statistics.exception.InvalidStatisticsRequestException;
import com.eav02.backend.statistics.service.StatisticsService;

@RestController
@RequestMapping("/api/v1/admin/statistics")
public class StatisticsController {

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final StatisticsService statistics;

    public StatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public StatisticsResponse getStatistics(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(name = "groupBy", required = false) String groupBy) {
        return statistics.getStatistics(parseDate("startDate", startDate),
                parseDate("endDate", endDate), StatisticsGroupBy.fromParameter(groupBy));
    }

    private LocalDate parseDate(String parameter, String value) {
        if (!ISO_DATE.matcher(value).matches()) {
            throw new InvalidStatisticsRequestException(parameter, "Debe tener formato YYYY-MM-DD");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException failure) {
            throw new InvalidStatisticsRequestException(parameter, "Debe ser una fecha válida");
        }
    }
}
