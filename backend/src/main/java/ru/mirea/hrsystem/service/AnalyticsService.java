package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.hrsystem.dto.AnalyticsDtos.NamedCount;
import ru.mirea.hrsystem.repository.VacancyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {
    private final VacancyRepository vacancies;
    private final PriorityCalculator priorityCalculator;

    public AnalyticsService(VacancyRepository vacancies, PriorityCalculator priorityCalculator) {
        this.vacancies = vacancies;
        this.priorityCalculator = priorityCalculator;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = vacancies.count();
        long active = vacancies.countByArchivedAtIsNull();
        long archived = vacancies.countByArchivedAtIsNotNull();
        long remote = vacancies.countByRemoteTrue();
        BigDecimal avg = vacancies.averageSalaryMin();
        if (avg != null) {
            avg = avg.setScale(2, RoundingMode.HALF_UP);
        } else {
            avg = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        List<NamedCount> byEmployer = vacancies.countByEmployer().stream()
                .map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
        List<NamedCount> byStatus = vacancies.countByStatus().stream()
                .map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        Map<Priority, Long> priorityCounts = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) priorityCounts.put(p, 0L);
        for (Vacancy vacancy : vacancies.findAll()) {
            Priority p = priorityCalculator.calculate(vacancy.getSalaryMin(), vacancy.getCreatedAt());
            priorityCounts.merge(p, 1L, Long::sum);
        }
        List<NamedCount> byPriority = new ArrayList<>();
        priorityCounts.forEach((k, v) -> byPriority.add(new NamedCount(k.name(), v)));

        return new AnalyticsResponse(total, active, archived, remote, avg, byEmployer, byStatus, byPriority);
    }
}
