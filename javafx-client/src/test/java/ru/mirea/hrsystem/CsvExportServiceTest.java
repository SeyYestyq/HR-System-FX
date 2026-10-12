package ru.mirea.hrsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.hrsystem.model.Vacancy;
import ru.mirea.hrsystem.model.VacancyStatus;
import ru.mirea.hrsystem.service.CsvExportService;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private final CsvExportService exportService = new CsvExportService();

    @Test
    @DisplayName("Экспорт CSV создает файл с корректным UTF-8 BOM и заголовком")
    void shouldExportWithBomAndHeader(@TempDir Path tempDir) throws Exception {
        File targetFile = tempDir.resolve("export_test.csv").toFile();

        Vacancy v1 = new Vacancy(1L, "Java Dev", "Spring & SQL",
                new BigDecimal("150000"), new BigDecimal("200000"), VacancyStatus.ACTIVE, 1L);
        v1.setEmployerCompanyName("Яндекс");

        exportService.export(List.of(v1), targetFile);

        assertThat(targetFile).exists();

        // Проверка BOM (0xEF, 0xBB, 0xBF)
        byte[] bytes = Files.readAllBytes(targetFile.toPath());
        assertThat(bytes.length).isGreaterThan(3);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        List<String> lines = Files.readAllLines(targetFile.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);
        // lines.get(0) содержит символ BOM \uFEFF в самом начале файла
        assertThat(lines.get(0)).isEqualTo("\uFEFFID;Название вакансии;Компания;Зарплата от;Зарплата до;Статус;Описание");
        assertThat(lines.get(1)).isEqualTo("1;Java Dev;Яндекс;150000;200000;ACTIVE;Spring & SQL");
    }

    @Test
    @DisplayName("Экранирование точек с запятой предотвращает сдвиг колонок в Excel (DoD День 6)")
    void shouldEscapeSemicolonsToPreventColumnShift(@TempDir Path tempDir) throws Exception {
        File targetFile = tempDir.resolve("semicolon_test.csv").toFile();

        Vacancy v = new Vacancy(2L, "Lead Developer; Java & Cloud", "Microservices; Docker; Kafka",
                new BigDecimal("300000"), new BigDecimal("400000"), VacancyStatus.ACTIVE, 2L);
        v.setEmployerCompanyName("Сбер;Инновации");

        exportService.export(List.of(v), targetFile);

        List<String> lines = Files.readAllLines(targetFile.toPath(), StandardCharsets.UTF_8);
        String dataLine = lines.get(1);

        // Разделяем по CSV_DELIMITER (;)
        String[] columns = dataLine.split(";", -1);
        // Должно быть ровно 7 колонок: ID, Название, Компания, Зарплата от, Зарплата до, Статус, Описание
        assertThat(columns).hasSize(7);
        assertThat(columns[0]).isEqualTo("2");
        assertThat(columns[1]).isEqualTo("Lead Developer, Java & Cloud");
        assertThat(columns[2]).isEqualTo("Сбер,Инновации");
        assertThat(columns[3]).isEqualTo("300000");
        assertThat(columns[4]).isEqualTo("400000");
        assertThat(columns[5]).isEqualTo("ACTIVE");
        assertThat(columns[6]).isEqualTo("Microservices, Docker, Kafka");
    }
}
