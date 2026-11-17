package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.domain.Priority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityCalculatorTest {
    private final PriorityCalculator calculator = new PriorityCalculator();

    @Test
    void highWhenSalaryAtLeast200k() {
        assertEquals(Priority.HIGH, calculator.calculate(new BigDecimal("200000"),
                Instant.now().minus(30, ChronoUnit.DAYS)));
    }

    @Test
    void highWhenYoungerThan7Days() {
        assertEquals(Priority.HIGH, calculator.calculate(new BigDecimal("50000"), Instant.now()));
    }

    @Test
    void mediumWhenSalaryAtLeast100k() {
        assertEquals(Priority.MEDIUM, calculator.calculate(new BigDecimal("150000"),
                Instant.now().minus(30, ChronoUnit.DAYS)));
    }

    @Test
    void lowOtherwise() {
        assertEquals(Priority.LOW, calculator.calculate(new BigDecimal("80000"),
                Instant.now().minus(30, ChronoUnit.DAYS)));
    }
}
