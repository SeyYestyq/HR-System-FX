package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Component;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;

import java.util.LinkedHashSet;

@Component
public class VacancyMapper {
    private final PriorityCalculator priorityCalculator;

    public VacancyMapper(PriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    public VacancyResponse toResponse(Vacancy vacancy) {
        // Копируем теги из PersistentSet, иначе Jackson падает после закрытия сессии (open-in-view: false).
        return new VacancyResponse(
                vacancy.getUuid(),
                vacancy.getCode(),
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getEmployer().getUuid(),
                vacancy.getEmployer().getName(),
                vacancy.getStatus(),
                vacancy.getSalaryMin(),
                vacancy.getSalaryMax(),
                vacancy.isRemote(),
                new LinkedHashSet<>(vacancy.getTags()),
                priorityCalculator.calculate(vacancy.getSalaryMin(), vacancy.getCreatedAt()),
                vacancy.isArchived(),
                vacancy.getArchivedAt(),
                vacancy.getCreatedAt(),
                vacancy.getUpdatedAt(),
                vacancy.getVersion()
        );
    }
}
