package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.model.StatisticsDto;
import ru.mirea.hrsystem.repository.InMemoryVacancyDao;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsServiceTest {

    @Test
    void stubVacanciesProduceFiveMetrics() {
        StatisticsDto dto = new StatisticsService(new InMemoryVacancyDao()).calculateMetrics();

        assertThat(dto.totalVacancies()).isEqualTo(8);
        assertThat(dto.activeVacancies()).isEqualTo(6);
        assertThat(dto.archivedVacancies()).isEqualTo(1);
        assertThat(dto.averageSalaryMin()).isEqualByComparingTo(new BigDecimal("175625"));
        assertThat(dto.maxSalary()).isEqualByComparingTo(new BigDecimal("480000"));
    }

    @Test
    void emptyListIsZeroWithoutDivisionError() {
        StatisticsDto dto = new StatisticsService(new InMemoryVacancyDao() {
            @Override
            public java.util.List<ru.mirea.hrsystem.model.Vacancy> findAll() {
                return java.util.List.of();
            }
        }).calculateMetrics();

        assertThat(dto.totalVacancies()).isZero();
        assertThat(dto.averageSalaryMin()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.maxSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
