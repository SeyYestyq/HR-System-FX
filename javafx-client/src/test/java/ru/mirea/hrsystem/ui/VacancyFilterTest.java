package ru.mirea.hrsystem.ui;

import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.repository.InMemoryVacancyDao;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyFilterTest {

    @Test
    void salaryUpTo100000KeepsOnlyLowMinimums() {
        List<BigDecimal> salaries = matched("", "Все статусы", "до 100 000").stream()
                .map(Vacancy::getSalaryMin)
                .toList();

        assertThat(salaries).containsExactlyInAnyOrder(new BigDecimal("80000"), new BigDecimal("45000"));
    }

    @Test
    void salaryFrom200000KeepsHighMinimums() {
        List<BigDecimal> salaries = matched("", "Все статусы", "от 200 000").stream()
                .map(Vacancy::getSalaryMin)
                .toList();

        assertThat(salaries).containsExactlyInAnyOrder(new BigDecimal("320000"), new BigDecimal("350000"));
    }

    @Test
    void springSearchFindsTitleAndDescription() {
        List<String> titles = matched("Spring", "Все статусы", "Вся зарплата").stream()
                .map(Vacancy::getTitle)
                .toList();

        assertThat(titles).containsExactlyInAnyOrder(
                "Java Backend Developer",
                "Middle Spring Boot Engineer");
    }

    @Test
    void emptyFiltersReturnAllStubVacancies() {
        assertThat(matched("", "Все статусы", "Вся зарплата")).hasSize(8);
    }

    private static List<Vacancy> matched(String query, String status, String salaryRange) {
        return new InMemoryVacancyDao().findAll().stream()
                .filter(vacancy -> MainController.matches(vacancy, query, status, salaryRange))
                .toList();
    }
}
