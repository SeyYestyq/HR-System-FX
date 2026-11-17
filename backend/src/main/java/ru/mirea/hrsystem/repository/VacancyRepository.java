package ru.mirea.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.hrsystem.domain.Vacancy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyRepository extends JpaRepository<Vacancy, Long>, JpaSpecificationExecutor<Vacancy> {
    Optional<Vacancy> findByUuid(UUID uuid);
    Optional<Vacancy> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    long countByArchivedAtIsNull();
    long countByArchivedAtIsNotNull();
    long countByRemoteTrue();

    @Query("select avg(v.salaryMin) from Vacancy v where v.salaryMin is not null")
    BigDecimal averageSalaryMin();

    @Query("select e.name, count(v) from Vacancy v join v.employer e group by e.name order by e.name")
    List<Object[]> countByEmployer();

    @Query("select v.status, count(v) from Vacancy v group by v.status")
    List<Object[]> countByStatus();
}
