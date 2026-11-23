package com.eav02.backend.statistics.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.eav02.backend.statistics.dto.StatisticsGroupBy;

@Repository
public class StatisticsRepository {

    private static final String AGGREGATE_SQL = """
            WITH events AS (
                SELECT date_trunc(:grouping, created_at AT TIME ZONE 'America/Bogota')::date AS period,
                       'users' AS metric
                FROM users.app_users
                WHERE created_at >= :startInclusive AND created_at < :endExclusive
                UNION ALL
                SELECT date_trunc(:grouping, created_at AT TIME ZONE 'America/Bogota')::date,
                       'projects'
                FROM projects.projects
                WHERE created_at >= :startInclusive AND created_at < :endExclusive
                UNION ALL
                SELECT date_trunc(:grouping, created_at AT TIME ZONE 'America/Bogota')::date,
                       'posts'
                FROM posts.posts
                WHERE created_at >= :startInclusive AND created_at < :endExclusive
                UNION ALL
                SELECT date_trunc(:grouping, created_at AT TIME ZONE 'America/Bogota')::date,
                       'comments'
                FROM interactions.comments
                WHERE created_at >= :startInclusive AND created_at < :endExclusive
            )
            SELECT period,
                   COUNT(*) FILTER (WHERE metric = 'users') AS users,
                   COUNT(*) FILTER (WHERE metric = 'projects') AS projects,
                   COUNT(*) FILTER (WHERE metric = 'posts') AS posts,
                   COUNT(*) FILTER (WHERE metric = 'comments') AS comments
            FROM events
            GROUP BY period
            ORDER BY period
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public StatisticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StatisticsAggregateRow> aggregate(
            Instant startInclusive, Instant endExclusive, StatisticsGroupBy groupBy) {
        var parameters = new MapSqlParameterSource()
                .addValue("startInclusive", OffsetDateTime.ofInstant(startInclusive, ZoneOffset.UTC))
                .addValue("endExclusive", OffsetDateTime.ofInstant(endExclusive, ZoneOffset.UTC))
                .addValue("grouping", groupBy.postgresUnit());

        return jdbc.query(AGGREGATE_SQL, parameters, (result, rowNumber) ->
                new StatisticsAggregateRow(
                        result.getObject("period", java.time.LocalDate.class),
                        result.getLong("users"),
                        result.getLong("projects"),
                        result.getLong("posts"),
                        result.getLong("comments")));
    }
}
