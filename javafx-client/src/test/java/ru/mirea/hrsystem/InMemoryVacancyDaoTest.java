package ru.mirea.hrsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.repository.InMemoryVacancyDao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVacancyDaoTest {

    private InMemoryVacancyDao dao;

    @BeforeEach
    void setUp() {
        dao = new InMemoryVacancyDao();
    }

    @Test
    @DisplayName("Инициализация стаба содержит ровно 8 строк данных")
    void shouldHaveExactEightInitialItems() {
        List<Vacancy> all = dao.findAll();
        assertThat(all).hasSize(8);
    }

    @Test
    @DisplayName("Создание новой вакансии генерирует ID и добавляет в список")
    void shouldCreateNewVacancy() {
        Vacancy v = new Vacancy(null, "Test Automation Lead", "Playwright & Java",
                new BigDecimal("220000"), new BigDecimal("300000"), VacancyStatus.ACTIVE, 1L);

        Vacancy created = dao.create(v);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(dao.findAll()).hasSize(9);
    }

    @Test
    @DisplayName("Поиск по ID возвращает существующую запись")
    void shouldFindById() {
        Optional<Vacancy> found = dao.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Java Backend Developer");
    }

    @Test
    @DisplayName("Обновление вакансии корректно сохраняет новые данные")
    void shouldUpdateVacancy() {
        Vacancy v = dao.findById(1L).orElseThrow();
        v.setTitle("Updated Title");
        v.setStatus(VacancyStatus.ARCHIVED);

        dao.update(v);

        Vacancy updated = dao.findById(1L).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo(VacancyStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Удаление вакансии удаляет её из репозитория")
    void shouldDeleteVacancy() {
        dao.delete(1L);

        assertThat(dao.findById(1L)).isEmpty();
        assertThat(dao.findAll()).hasSize(7);
    }
}
