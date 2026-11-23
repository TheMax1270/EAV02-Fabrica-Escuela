package com.eav02.backend.statistics.dto;

import com.eav02.backend.statistics.exception.InvalidStatisticsRequestException;

public enum StatisticsGroupBy {
    WEEK("week"),
    MONTH("month");

    private final String postgresUnit;

    StatisticsGroupBy(String postgresUnit) {
        this.postgresUnit = postgresUnit;
    }

    public String postgresUnit() {
        return postgresUnit;
    }

    public static StatisticsGroupBy fromParameter(String value) {
        if (value == null) {
            return MONTH;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new InvalidStatisticsRequestException("groupBy", "Debe ser WEEK o MONTH");
        }
    }
}
