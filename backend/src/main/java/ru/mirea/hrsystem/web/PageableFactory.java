package ru.mirea.hrsystem.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.mirea.hrsystem.error.BadRequestException;

import java.util.Map;
import java.util.Set;

@Component
public class PageableFactory {
    private static final Set<String> VACANCY_SORT = Set.of("createdAt", "salaryMax", "title");
    private static final Map<String, String> VACANCY_MAP = Map.of(
            "createdAt", "createdAt",
            "salaryMax", "salaryMax",
            "title", "title"
    );

    public Pageable vacancies(int page, int size, String sort) {
        return build(page, size, sort, VACANCY_SORT, VACANCY_MAP, "createdAt,desc");
    }

    public Pageable users(int page, int size, String sort) {
        return build(page, size, sort, Set.of("createdAt", "email", "fullName"),
                Map.of("createdAt", "createdAt", "email", "email", "fullName", "fullName"),
                "createdAt,desc");
    }

    public Pageable audit(int page, int size, String sort) {
        return build(page, size, sort, Set.of("createdAt", "eventType"),
                Map.of("createdAt", "createdAt", "eventType", "eventType"),
                "createdAt,desc");
    }

    private Pageable build(int page, int size, String sort, Set<String> allowed,
                           Map<String, String> mapping, String defaultSort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String raw = (sort == null || sort.isBlank()) ? defaultSort : sort;
        String[] parts = raw.split(",");
        String field = parts[0].trim();
        if (!allowed.contains(field)) {
            throw new BadRequestException("Недопустимое поле сортировки: " + field);
        }
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, mapping.get(field)));
    }
}
