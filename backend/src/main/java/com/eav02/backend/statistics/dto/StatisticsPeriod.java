package com.eav02.backend.statistics.dto;

public record StatisticsPeriod(String period, long users, long projects, long posts, long comments) {
}
