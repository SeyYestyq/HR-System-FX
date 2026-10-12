package ru.mirea.hrsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.model.StatisticsDto;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.repository.InMemoryVacancyDao;
import ru.mirea.hrsystem.repository.VacancyDao;
import ru.mirea.hrsystem.service.StatisticsService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsServiceTest {

    private StatisticsService statisticsService;
    private VacancyDao inMemoryDao;

    @BeforeEach
    void setUp() {
        inMemoryDao = new InMemoryVacancyDao();
        statisticsService = new StatisticsService(inMemoryDao);
    }

    @Test
    @DisplayName("Корректный расчет 5 метрик для стандартного набора данных (Stub)")
    void shouldCalculateAccurateMetricsForStubData() {
        StatisticsDto dto = statisticsService.calculateMetrics();

        assertThat(dto.totalVacancies()).isEqualTo(8);
        assertThat(dto.activeVacancies()).isEqualTo(6);
        assertThat(dto.archivedVacancies()).isEqualTo(1);
        assertThat(dto.averageSalaryMin().doubleValue()).isEqualTo(175625.0);
        assertThat(dto.maxSalary()).isEqualByComparingTo(new BigDecimal("480000"));
    }

    @Test
    @DisplayName("Корректная обработка пустого репозитория без деления на ноль")
    void shouldHandleEmptyRepositorySafely() {
        VacancyDao emptyDao = new VacancyDao() {
            @Override public List<Vacancy> findAll() { return Collections.emptyList(); }
            @Override public Optional<Vacancy> findById(Long id) { return Optional.empty(); }
            @Override public Vacancy create(Vacancy v) { return v; }
            @Override public void update(Vacancy v) {}
            @Override public void delete(Long id) {}
        };

        StatisticsService service = new StatisticsService(emptyDao);
        StatisticsDto dto = service.calculateMetrics();

        assertThat(dto.totalVacancies()).isZero();
        assertThat(dto.activeVacancies()).isZero();
        assertThat(dto.archivedVacancies()).isZero();
        assertThat(dto.averageSalaryMin().doubleValue()).isZero();
        assertThat(dto.maxSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Корректный расчет метрик при наличии вакансий с null зарплатой")
    void shouldCalculateMetricsWithNullSalaries() {
        VacancyDao dao = new VacancyDao() {
            @Override
            public List<Vacancy> findAll() {
                return List.of(
                        new Vacancy(1L, "Junior", "Desc", null, null, VacancyStatus.ACTIVE, 1L),
                        new Vacancy(2L, "Middle", "Desc", new BigDecimal("100000"), new BigDecimal("150000"), VacancyStatus.ACTIVE, 1L),
                        new Vacancy(3L, "Senior", "Desc", new BigDecimal("200000"), new BigDecimal("300000"), VacancyStatus.ARCHIVED, 1L)
                );
            }
            @Override public Optional<Vacancy> findById(Long id) { return Optional.empty(); }
            @Override public Vacancy create(Vacancy v) { return v; }
            @Override public void update(Vacancy v) {}
            @Override public void delete(Long id) {}
        };

        StatisticsService service = new StatisticsService(dao);
        StatisticsDto dto = service.calculateMetrics();

        assertThat(dto.totalVacancies()).isEqualTo(3);
        assertThat(dto.activeVacancies()).isEqualTo(2);
        assertThat(dto.archivedVacancies()).isEqualTo(1);
        assertThat(dto.averageSalaryMin().doubleValue()).isEqualTo(150000.0);
        assertThat(dto.maxSalary()).isEqualByComparingTo(new BigDecimal("300000"));
    }
}
