package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.domain.Priority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityCalculatorPropertyTest {
    private final PriorityCalculator calculator = new PriorityCalculator();
    private final Random random = new Random(42);

    @Test
    void randomInputsAlwaysProduceValidPriority() {
        for (int i = 0; i < 5000; i++) {
            BigDecimal salary = random.nextBoolean() ? null : BigDecimal.valueOf(random.nextInt(0, 500_001));
            Instant created = Instant.now().minus(random.nextInt(0, 60), ChronoUnit.DAYS);
            Priority p = calculator.calculate(salary, created);
            assertNotNull(p);
            assertTrue(p == Priority.HIGH || p == Priority.MEDIUM || p == Priority.LOW);
        }
    }

    @Test
    void youngVacancyIsAlwaysHighRegardlessOfSalary() {
        for (int i = 0; i < 1000; i++) {
            BigDecimal salary = BigDecimal.valueOf(random.nextInt(0, 99_999));
            Instant created = Instant.now().minus(random.nextInt(0, 6), ChronoUnit.DAYS);
            assertEquals(Priority.HIGH, calculator.calculate(salary, created));
        }
    }

    @Test
    void highSalaryOldVacancyIsAlwaysHigh() {
        for (int i = 0; i < 1000; i++) {
            BigDecimal salary = BigDecimal.valueOf(200_000 + random.nextInt(0, 100_000));
            Instant created = Instant.now().minus(10 + random.nextInt(0, 100), ChronoUnit.DAYS);
            assertEquals(Priority.HIGH, calculator.calculate(salary, created));
        }
    }

    @Test
    void mediumBandIsDeterministicForOldVacancies() {
        Instant old = Instant.now().minus(30, ChronoUnit.DAYS);
        for (int salary = 100_000; salary < 200_000; salary += 137) {
            assertEquals(Priority.MEDIUM, calculator.calculate(BigDecimal.valueOf(salary), old));
        }
        for (int salary = 0; salary < 100_000; salary += 211) {
            assertEquals(Priority.LOW, calculator.calculate(BigDecimal.valueOf(salary), old));
        }
    }

    @Test
    void calculateIsIdempotent() {
        for (int i = 0; i < 1000; i++) {
            BigDecimal salary = BigDecimal.valueOf(random.nextInt(0, 400_000));
            Instant created = Instant.now().minus(random.nextInt(0, 40), ChronoUnit.DAYS);
            assertEquals(calculator.calculate(salary, created), calculator.calculate(salary, created));
        }
    }
}
