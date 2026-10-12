package ru.mirea.hrsystem.service;

import ru.mirea.hrsystem.model.Vacancy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExportService {

    public void export(List<Vacancy> vacancies, File targetFile) throws Exception {
        try (FileOutputStream output = new FileOutputStream(targetFile);
             OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             PrintWriter printer = new PrintWriter(writer)) {
            output.write(0xEF);
            output.write(0xBB);
            output.write(0xBF);
            printer.println("ID;Название вакансии;Компания;Зарплата от;Зарплата до;Статус;Описание");
            for (Vacancy vacancy : vacancies) {
                printer.printf("%s;%s;%s;%s;%s;%s;%s%n",
                        vacancy.getId() == null ? "" : vacancy.getId(),
                        cell(vacancy.getTitle()),
                        cell(vacancy.getEmployerCompanyName()),
                        cell(money(vacancy.getSalaryMin())),
                        cell(money(vacancy.getSalaryMax())),
                        cell(vacancy.getStatus() == null ? "" : vacancy.getStatus().name()),
                        cell(vacancy.getDescription()));
            }
        }
    }

    private static String money(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    static String cell(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }
}
