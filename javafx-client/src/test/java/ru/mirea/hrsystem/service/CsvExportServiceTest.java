package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    @TempDir
    Path directory;

    @Test
    void quotesFieldWithSemicolon() {
        assertThat(CsvExportService.cell("Java; Backend")).isEqualTo("\"Java; Backend\"");
        assertThat(CsvExportService.cell("обычное")).isEqualTo("обычное");
    }

    @Test
    void writesHeaderAndVisibleRow() throws Exception {
        Vacancy vacancy = new Vacancy(8L, "Java; Backend", "Spring",
                new BigDecimal("160000"), new BigDecimal("240000"), VacancyStatus.ACTIVE, 1L);
        vacancy.setEmployerCompanyName("Яндекс");
        Path file = directory.resolve("vacancies.csv");

        new CsvExportService().export(java.util.List.of(vacancy), file.toFile());

        String text = Files.readString(file);
        assertThat(text).startsWith("\uFEFFID;Название вакансии;Компания;Зарплата от;Зарплата до;Статус;Описание");
        assertThat(text).contains("\"Java; Backend\";Яндекс;160000;240000;ACTIVE;Spring");
    }
}
