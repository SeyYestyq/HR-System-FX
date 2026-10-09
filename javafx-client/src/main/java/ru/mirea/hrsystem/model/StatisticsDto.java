package ru.mirea.hrsystem.model;

import java.math.BigDecimal;

public record StatisticsDto(
        long totalVacancies,
        long activeVacancies,
        long archivedVacancies,
        BigDecimal averageSalaryMin,
        BigDecimal maxSalary
) {
    public static StatisticsDto empty() {
        return new StatisticsDto(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
