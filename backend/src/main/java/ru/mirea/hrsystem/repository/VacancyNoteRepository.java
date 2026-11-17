package ru.mirea.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.hrsystem.domain.VacancyNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyNoteRepository extends JpaRepository<VacancyNote, Long> {
    List<VacancyNote> findByVacancyUuidOrderByPinnedDescCreatedAtDesc(UUID vacancyUuid);
    Optional<VacancyNote> findByUuid(UUID uuid);
    void deleteByVacancyId(Long vacancyId);
}
