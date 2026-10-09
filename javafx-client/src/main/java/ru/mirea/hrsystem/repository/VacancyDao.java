package ru.mirea.hrsystem.repository;

import ru.mirea.hrsystem.model.Vacancy;
import java.util.List;
import java.util.Optional;

public interface VacancyDao {
    List<Vacancy> findAll();
    Optional<Vacancy> findById(Long id);
    Vacancy create(Vacancy vacancy);
    void update(Vacancy vacancy);
    void delete(Long id);
}
