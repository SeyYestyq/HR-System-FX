package ru.mirea.hrsystem.filter;

import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.VacancyStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record VacancyFilter(
        String q,
        UUID employerUuid,
        VacancyStatus status,
        Boolean remote,
        Priority priority,
        String tag,
        Boolean archived,
        BigDecimal salaryFrom,
        BigDecimal salaryTo
) {}
