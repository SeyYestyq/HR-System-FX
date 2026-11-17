package ru.mirea.hrsystem.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.VacancyRepository;
import ru.mirea.hrsystem.service.PriorityCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Дифференциальный тест: фильтр по priority в Specification
 * должен совпадать с PriorityCalculator на тех же данных.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriorityFilterConsistencyTest {
    @Autowired VacancyRepository vacancies;
    @Autowired EmployerRepository employers;
    @Autowired PriorityCalculator calculator;

    @BeforeEach
    void seed() {
        vacancies.deleteAll();
        employers.deleteAll();
        Employer employer = new Employer(UUID.randomUUID(), "Diff Emp");
        employer.setCode("DIFF");
        employer = employers.saveAndFlush(employer);

        save(employer, "HIGH-SAL", "250000", Instant.now().minus(40, ChronoUnit.DAYS));
        save(employer, "MED-SAL", "150000", Instant.now().minus(40, ChronoUnit.DAYS));
        save(employer, "LOW-SAL", "50000", Instant.now().minus(40, ChronoUnit.DAYS));
        save(employer, "YOUNG-LOW", "40000", Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private void save(Employer employer, String code, String salary, Instant createdAt) {
        Vacancy v = new Vacancy(UUID.randomUUID(), code, code, employer);
        v.setStatus(VacancyStatus.OPEN);
        v.setSalaryMin(new BigDecimal(salary));
        v.setSalaryMax(new BigDecimal(salary).add(new BigDecimal("10000")));
        v.setCreatedAt(createdAt);
        vacancies.saveAndFlush(v);
    }

    @Test
    void filterMatchesCalculatorForAllPriorityBands() {
        List<Vacancy> all = vacancies.findAll();
        for (Priority priority : Priority.values()) {
            var filtered = vacancies.findAll(
                    VacancySpecifications.from(new VacancyFilter(null, null, null, null, priority, null, null, null, null)),
                    PageRequest.of(0, 100)).getContent();
            for (Vacancy vacancy : all) {
                Priority calculated = calculator.calculate(vacancy.getSalaryMin(), vacancy.getCreatedAt());
                boolean inFilter = filtered.stream().anyMatch(x -> x.getUuid().equals(vacancy.getUuid()));
                assertEquals(calculated == priority, inFilter,
                        () -> "Mismatch for " + vacancy.getCode() + " expected=" + calculated + " filter=" + priority);
            }
            assertTrue(filtered.stream().allMatch(v ->
                    calculator.calculate(v.getSalaryMin(), v.getCreatedAt()) == priority));
        }
    }
}
