package ru.mirea.hrsystem.dto;

import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record NamedCount(String name, long count) {}

    public record AnalyticsResponse(
            long total,
            long active,
            long archived,
            long remoteCount,
            BigDecimal averageSalaryMin,
            List<NamedCount> byEmployer,
            List<NamedCount> byStatus,
            List<NamedCount> byPriority
    ) {}
}
