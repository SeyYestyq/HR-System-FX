package ru.mirea.hrsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.exception.BusinessException;
import ru.mirea.hrsystem.exception.ValidationException;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.repository.InMemoryVacancyDao;
import ru.mirea.hrsystem.repository.VacancyDao;
import ru.mirea.hrsystem.service.VacancyService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VacancyServiceTest {
    private VacancyService service;
    private VacancyDao inMemoryDao;

    @BeforeEach
    void setUp() {
        inMemoryDao = new InMemoryVacancyDao();
        service = new VacancyService(inMemoryDao);
    }

    @Test
    @DisplayName("Успешное создание корректной вакансии")
    void shouldCreateValidVacancy() {
        Vacancy vacancy = new Vacancy(null, "Middle Java Developer", "Spring & PostgreSQL",
                new BigDecimal("150000"), new BigDecimal("200000"), VacancyStatus.ACTIVE, 1L);

        Vacancy created = service.createVacancy(vacancy);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Middle Java Developer");
        assertThat(service.getAllVacancies()).isNotEmpty();
    }

    @Test
    @DisplayName("Ошибка валидации: зарплата 'до' меньше зарплаты 'от'")
    void shouldThrowExceptionWhenMaxSalaryIsLowerThanMin() {
        Vacancy invalid = new Vacancy(null, "QA Engineer", "Testing",
                new BigDecimal("200000"), new BigDecimal("100000"), VacancyStatus.ACTIVE, 1L);

        assertThatThrownBy(() -> service.createVacancy(invalid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не может быть меньше");
    }

    @Test
    @DisplayName("Ошибка валидации: название вакансии слишком короткое")
    void shouldThrowExceptionWhenTitleIsTooShort() {
        Vacancy invalid = new Vacancy(null, "AB", "Testing",
                new BigDecimal("50000"), new BigDecimal("100000"), VacancyStatus.ACTIVE, 1L);

        assertThatThrownBy(() -> service.createVacancy(invalid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("минимум 3 символа");
    }

    @Test
    @DisplayName("Ошибка валидации: не указан работодатель")
    void shouldThrowExceptionWhenEmployerNotSet() {
        Vacancy invalid = new Vacancy(null, "Java Architect", "Testing",
                new BigDecimal("50000"), new BigDecimal("100000"), VacancyStatus.ACTIVE, null);

        assertThatThrownBy(() -> service.createVacancy(invalid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Необходимо выбрать работодателя");
    }

    @Test
    @DisplayName("Ошибка бизнес-правила: повторная архивация вакансии")
    void shouldThrowExceptionWhenReArchivingVacancy() {
        Vacancy vacancy = service.createVacancy(new Vacancy(null, "DevOps Engineer", "Docker",
                new BigDecimal("180000"), new BigDecimal("250000"), VacancyStatus.ACTIVE, 1L));

        service.archiveVacancy(vacancy.getId());

        assertThatThrownBy(() -> service.archiveVacancy(vacancy.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("уже находится в архиве");
    }
}
