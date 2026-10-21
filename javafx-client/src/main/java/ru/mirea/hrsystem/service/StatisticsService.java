package ru.mirea.hrsystem.service;

import ru.mirea.hrsystem.model.StatisticsDto;
import ru.mirea.hrsystem.repository.VacancyDao;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StatisticsService {
    private final VacancyDao vacancyDao;

    public StatisticsService(VacancyDao vacancyDao) {
        this.vacancyDao = vacancyDao;
    }

    public StatisticsDto calculateMetrics() {

        List<Vacancy> vacancies = vacancyDao.findAll();
        long total = vacancies.size();
        long active  = vacancies.stream().filter(vac -> vac.getStatus() == VacancyStatus.ACTIVE).count();
        long archived = vacancies.stream().filter(vac -> vac.getStatus() == VacancyStatus.ARCHIVED).count();
        BigDecimal maxSalary = vacancies.stream()
        .map(Vacancy::getSalaryMax)
        .filter(Objects::nonNull)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

        double avg = vacancies.stream()
        .map(Vacancy::getSalaryMin)
        .filter(Objects::nonNull)
        .mapToDouble(BigDecimal::doubleValue)
        .average()
        .orElse(0.0);
        BigDecimal avgSalary = BigDecimal.valueOf(avg);

        return new StatisticsDto(total, active, archived, avgSalary, maxSalary);
    }
}
