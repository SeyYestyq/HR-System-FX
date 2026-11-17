package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Component;
import ru.mirea.hrsystem.domain.Priority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PriorityCalculator {
    private static final BigDecimal HIGH_SALARY = new BigDecimal("200000");
    private static final BigDecimal MEDIUM_SALARY = new BigDecimal("100000");

    public Priority calculate(BigDecimal salaryMin, Instant createdAt) {
        Instant created = createdAt == null ? Instant.now() : createdAt;
        boolean young = created.isAfter(Instant.now().minus(7, ChronoUnit.DAYS));
        if (young || (salaryMin != null && salaryMin.compareTo(HIGH_SALARY) >= 0)) {
            return Priority.HIGH;
        }
        if (salaryMin != null && salaryMin.compareTo(MEDIUM_SALARY) >= 0) {
            return Priority.MEDIUM;
        }
        return Priority.LOW;
    }
}
