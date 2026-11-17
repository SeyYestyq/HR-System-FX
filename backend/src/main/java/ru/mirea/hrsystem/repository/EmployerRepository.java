package ru.mirea.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.hrsystem.domain.Employer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployerRepository extends JpaRepository<Employer, Long> {
    Optional<Employer> findByUuid(UUID uuid);
    List<Employer> findByEnabledTrueOrderByNameAsc();
    List<Employer> findAllByOrderByNameAsc();
    Optional<Employer> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
