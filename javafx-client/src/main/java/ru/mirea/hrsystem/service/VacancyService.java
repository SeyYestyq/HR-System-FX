package ru.mirea.hrsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.exception.BusinessException;
import ru.mirea.hrsystem.exception.ValidationException;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.repository.VacancyDao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class VacancyService {
    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);
    private final VacancyDao vacancyDao;

    public VacancyService(VacancyDao vacancyDao) {
        this.vacancyDao = Objects.requireNonNull(vacancyDao, "VacancyDao cannot be null");
    }

    public List<Vacancy> getAllVacancies() {
        log.info("Загрузка всех вакансий через сервис");
        return vacancyDao.findAll();
    }

    public Vacancy createVacancy(Vacancy vacancy) {
        validateVacancy(vacancy);
        log.info("Создание новой вакансии: '{}'", vacancy.getTitle());
        return vacancyDao.create(vacancy);
    }

    public void updateVacancy(Vacancy vacancy) {
        if (vacancy.getId() == null) {
            throw new ValidationException("Невозможно обновить вакансию без ID");
        }
        validateVacancy(vacancy);
        log.info("Обновление вакансии ID={}", vacancy.getId());
        vacancyDao.update(vacancy);
    }

    public void deleteVacancy(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Некорректный ID вакансии для удаления");
        }
        log.info("Удаление вакансии ID={}", id);
        vacancyDao.delete(id);
    }

    public void archiveVacancy(Long id) {
        Vacancy vacancy = vacancyDao.findById(id)
                .orElseThrow(() -> new BusinessException("Вакансия с ID=" + id + " не найдена"));

        if (vacancy.getStatus() == VacancyStatus.ARCHIVED) {
            throw new BusinessException("Вакансия ID=" + id + " уже находится в архиве!");
        }

        vacancy.setStatus(VacancyStatus.ARCHIVED);
        vacancyDao.update(vacancy);
        log.info("Вакансия ID={} успешно переведена в архив", id);
    }

    public void validateVacancy(Vacancy vacancy) {
        if (vacancy == null) {
            throw new ValidationException("Объект вакансии не может быть null");
        }
        if (vacancy.getTitle() == null || vacancy.getTitle().trim().length() < 3) {
            throw new ValidationException("Название вакансии обязательно (минимум 3 символа)");
        }
        if (vacancy.getTitle().trim().length() > 100) {
            throw new ValidationException("Название вакансии не может превышать 100 символов");
        }
        if (vacancy.getEmployerId() == null) {
            throw new ValidationException("Необходимо выбрать работодателя (компанию)");
        }

        BigDecimal min = vacancy.getSalaryMin();
        BigDecimal max = vacancy.getSalaryMax();

        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Минимальная зарплата не может быть отрицательной");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Максимальная зарплата не может быть отрицательной");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new ValidationException("Максимальная зарплата (" + max + ") не может быть меньше минимальной (" + min + ")");
        }
    }
}
