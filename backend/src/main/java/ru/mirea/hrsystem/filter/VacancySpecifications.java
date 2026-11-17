package ru.mirea.hrsystem.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.Vacancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VacancySpecifications {
    private VacancySpecifications() {}

    public static Specification<Vacancy> from(VacancyFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + filter.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)
                ));
            }
            if (filter.employerUuid() != null) {
                predicates.add(cb.equal(root.get("employer").get("uuid"), filter.employerUuid()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.remote() != null) {
                predicates.add(cb.equal(root.get("remote"), filter.remote()));
            }
            if (filter.tag() != null && !filter.tag().isBlank()) {
                Join<Object, Object> tags = root.join("tags");
                predicates.add(cb.equal(cb.lower(tags.as(String.class)), filter.tag().trim().toLowerCase(Locale.ROOT)));
                query.distinct(true);
            }
            if (filter.archived() != null) {
                if (filter.archived()) {
                    predicates.add(cb.isNotNull(root.get("archivedAt")));
                } else {
                    predicates.add(cb.isNull(root.get("archivedAt")));
                }
            }
            if (filter.salaryFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMin"), filter.salaryFrom()));
            }
            if (filter.salaryTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMax"), filter.salaryTo()));
            }
            if (filter.priority() != null) {
                predicates.add(priorityPredicate(root, cb, filter.priority()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate priorityPredicate(jakarta.persistence.criteria.Root<Vacancy> root,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               Priority priority) {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        BigDecimal high = new BigDecimal("200000");
        BigDecimal medium = new BigDecimal("100000");
        var young = cb.greaterThan(root.get("createdAt"), weekAgo);
        var highSalary = cb.and(cb.isNotNull(root.get("salaryMin")),
                cb.greaterThanOrEqualTo(root.get("salaryMin"), high));
        var mediumSalary = cb.and(cb.isNotNull(root.get("salaryMin")),
                cb.greaterThanOrEqualTo(root.get("salaryMin"), medium),
                cb.lessThan(root.get("salaryMin"), high));
        return switch (priority) {
            case HIGH -> cb.or(young, highSalary);
            case MEDIUM -> cb.and(cb.not(young), mediumSalary);
            case LOW -> cb.and(
                    cb.not(young),
                    cb.or(cb.isNull(root.get("salaryMin")), cb.lessThan(root.get("salaryMin"), medium))
            );
        };
    }
}
