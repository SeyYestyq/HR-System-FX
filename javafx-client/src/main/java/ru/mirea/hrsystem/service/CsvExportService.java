package ru.mirea.hrsystem.service;

import ru.mirea.hrsystem.model.Vacancy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Сервис выгрузки вакансий в формат CSV (Comma-Separated Values).
 * <p>
 * Архитектурные требования:
 * 1. Полная изоляция от слоя представления: никаких импортов графических библиотек UI.
 * 2. Запись UTF-8 BOM (0xEF, 0xBB, 0xBF) для корректного распознавания кириллицы в MS Excel.
 * 3. Экранирование разделителей (';') и переводов строк в текстовых данных.
 */
public class CsvExportService {

    private static final String CSV_DELIMITER = ";";
    private static final String CSV_HEADER = "ID;Название вакансии;Компания;Зарплата от;Зарплата до;Статус;Описание";

    /**
     * Экспортирует переданный список вакансий в целевой CSV-файл.
     *
     * @param vacancies  снимок (snapshot) списка вакансий
     * @param targetFile целевой файл на диске
     * @throws IOException при ошибках ввода-вывода
     */
    public void export(List<Vacancy> vacancies, File targetFile) throws IOException {
        if (targetFile == null) {
            throw new IllegalArgumentException("Целевой файл для экспорта не может быть null");
        }

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             PrintWriter writer = new PrintWriter(bw)) {

            // Запись UTF-8 Byte Order Mark (BOM)
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            writer.println(CSV_HEADER);

            if (vacancies != null) {
                for (Vacancy v : vacancies) {
                    writer.println(formatCsvRow(v));
                }
            }
            writer.flush();
        }
    }

    private String formatCsvRow(Vacancy v) {
        if (v == null) {
            return "";
        }
        return String.join(CSV_DELIMITER,
                String.valueOf(v.getId() != null ? v.getId() : ""),
                cleanField(v.getTitle()),
                cleanField(v.getEmployerCompanyName()),
                v.getSalaryMin() != null ? v.getSalaryMin().toPlainString() : "",
                v.getSalaryMax() != null ? v.getSalaryMax().toPlainString() : "",
                v.getStatus() != null ? v.getStatus().name() : "",
                cleanField(v.getDescription())
        );
    }

    private String cleanField(String value) {
        if (value == null) {
            return "";
        }
        // Заменяем разделители и переносы строк, чтобы не нарушать CSV-структуру
        return value.replace(";", ",")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}
