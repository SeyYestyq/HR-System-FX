package ru.mirea.hrsystem.repository;

import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryVacancyDao implements VacancyDao {
    private final List<Vacancy> storage = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public InMemoryVacancyDao() {
        seedInitialData();
    }

    private void seedInitialData() {
        storage.add(new Vacancy(1L, "Java Backend Developer", "Spring Boot микросервисы и PostgreSQL",
                new BigDecimal("160000"), new BigDecimal("240000"), VacancyStatus.ACTIVE, 1L));
        storage.get(0).setEmployerCompanyName("Яндекс");

        storage.add(new Vacancy(2L, "Senior Java / Kotlin Архитектор", "Высоконагруженные системы",
                new BigDecimal("320000"), new BigDecimal("450000"), VacancyStatus.ACTIVE, 2L));
        storage.get(1).setEmployerCompanyName("СберТех");

        storage.add(new Vacancy(3L, "Junior Java QA Automation", "JUnit 5, Selenide, CI/CD",
                new BigDecimal("80000"), new BigDecimal("110000"), VacancyStatus.ACTIVE, 3L));
        storage.get(2).setEmployerCompanyName("Т-Банк");

        storage.add(new Vacancy(4L, "Team Lead Java разработки", "Управление командой и Agile",
                new BigDecimal("350000"), new BigDecimal("480000"), VacancyStatus.ARCHIVED, 1L));
        storage.get(3).setEmployerCompanyName("Яндекс");

        storage.add(new Vacancy(5L, "Java Desktop Developer (JavaFX)", "Разработка GUI на JavaFX",
                new BigDecimal("140000"), new BigDecimal("200000"), VacancyStatus.ACTIVE, 4L));
        storage.get(4).setEmployerCompanyName("Озон");

        storage.add(new Vacancy(6L, "Middle Spring Boot Engineer", "Интеграция с Kafka и платежными шлюзами",
                new BigDecimal("190000"), new BigDecimal("260000"), VacancyStatus.ACTIVE, 3L));
        storage.get(5).setEmployerCompanyName("Т-Банк");

        storage.add(new Vacancy(7L, "Стажер Java Developer", "Обучение в команде, менторство",
                new BigDecimal("45000"), new BigDecimal("65000"), VacancyStatus.ACTIVE, 2L));
        storage.get(6).setEmployerCompanyName("СберТех");

        storage.add(new Vacancy(8L, "Legacy Java 8 Support Specialist", "Поддержка старых монолитных систем",
                new BigDecimal("120000"), new BigDecimal("150000"), VacancyStatus.REJECTED, 4L));
        storage.get(7).setEmployerCompanyName("Озон");
    }

    @Override
    public List<Vacancy> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public Optional<Vacancy> findById(Long id) {
        return storage.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    @Override
    public Vacancy create(Vacancy vacancy) {
        long newId = idGenerator.incrementAndGet();
        vacancy.setId(newId);
        vacancy.setCreatedAt(LocalDateTime.now());
        storage.add(0, vacancy);
        return vacancy;
    }

    @Override
    public void update(Vacancy vacancy) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).getId().equals(vacancy.getId())) {
                storage.set(i, vacancy);
                return;
            }
        }
    }

    @Override
    public void delete(Long id) {
        storage.removeIf(v -> v.getId().equals(id));
    }
}
