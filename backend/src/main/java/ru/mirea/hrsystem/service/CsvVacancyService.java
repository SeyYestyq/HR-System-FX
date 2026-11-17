package ru.mirea.hrsystem.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.dto.ImportDtos.ImportMode;
import ru.mirea.hrsystem.dto.ImportDtos.ImportPreview;
import ru.mirea.hrsystem.dto.ImportDtos.ImportResult;
import ru.mirea.hrsystem.dto.ImportDtos.RowIssue;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.filter.VacancyFilter;
import ru.mirea.hrsystem.repository.EmployerRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CsvVacancyService {
    private static final String[] HEADERS = {
            "code", "title", "description", "employerUuid", "status",
            "salaryMin", "salaryMax", "remote", "tags"
    };

    private final VacancyService vacancyService;
    private final EmployerRepository employers;

    public CsvVacancyService(VacancyService vacancyService, EmployerRepository employers) {
        this.vacancyService = vacancyService;
        this.employers = employers;
    }

    @Transactional(readOnly = true)
    public byte[] export(VacancyFilter filter) {
        List<VacancyResponse> rows = vacancyService.search(filter, Pageable.unpaged()).getContent();
        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (VacancyResponse row : rows) {
                printer.printRecord(
                        row.code(), row.title(), row.description(), row.employerUuid(),
                        row.status(), row.salaryMin(), row.salaryMax(), row.remote(),
                        String.join("|", row.tags() == null ? List.of() : row.tags())
                );
            }
            printer.flush();
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("Не удалось сформировать CSV");
        }
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        Parsed parsed = parse(file, mode);
        return new ImportPreview(parsed.total, parsed.valid.size(), parsed.issues.size(), parsed.issues);
    }

    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        Parsed parsed = parse(file, mode);
        int applied = 0;
        int skipped = 0;
        List<RowIssue> issues = new ArrayList<>(parsed.issues);
        for (ParsedRow row : parsed.valid) {
            try {
                Vacancy existing = vacancyService.findByCode(row.code);
                if (mode == ImportMode.CREATE_ONLY && existing != null) {
                    skipped++;
                    issues.add(new RowIssue(row.line, "Код уже существует"));
                    continue;
                }
                vacancyService.upsertFromImport(existing, row.request, row.code);
                applied++;
            } catch (RuntimeException ex) {
                skipped++;
                issues.add(new RowIssue(row.line, ex.getMessage()));
            }
        }
        return new ImportResult(applied, skipped, issues);
    }

    private Parsed parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл CSV пуст");
        }
        List<RowIssue> issues = new ArrayList<>();
        List<ParsedRow> valid = new ArrayList<>();
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build().parse(reader)) {
            for (CSVRecord record : parser) {
                total++;
                int line = (int) record.getRecordNumber() + 1;
                try {
                    String code = value(record, "code");
                    String title = value(record, "title");
                    if (title == null || title.length() < 3) {
                        throw new BadRequestException("title обязателен (3+ символов)");
                    }
                    UUID employerUuid = UUID.fromString(value(record, "employerUuid"));
                    if (employers.findByUuid(employerUuid).isEmpty()) {
                        throw new BadRequestException("Работодатель не найден");
                    }
                    VacancyStatus status = VacancyStatus.valueOf(value(record, "status").toUpperCase(Locale.ROOT));
                    BigDecimal salaryMin = parseDecimal(value(record, "salaryMin"));
                    BigDecimal salaryMax = parseDecimal(value(record, "salaryMax"));
                    boolean remote = Boolean.parseBoolean(value(record, "remote"));
                    Set<String> tags = parseTags(value(record, "tags"));
                    if (mode == ImportMode.UPSERT_BY_CODE && (code == null || code.isBlank())) {
                        throw new BadRequestException("code обязателен для UPSERT_BY_CODE");
                    }
                    VacancyRequest request = new VacancyRequest(title, value(record, "description"),
                            employerUuid, status, salaryMin, salaryMax, remote, tags, null);
                    valid.add(new ParsedRow(line, code, request));
                } catch (Exception ex) {
                    issues.add(new RowIssue(line, ex.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать CSV");
        }
        return new Parsed(total, valid, issues);
    }

    private String value(CSVRecord record, String name) {
        if (!record.isMapped(name)) return null;
        String value = record.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private Set<String> parseTags(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.stream(value.split("[|;,]"))
                .map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT)).toList());
    }

    private record Parsed(int total, List<ParsedRow> valid, List<RowIssue> issues) {}
    private record ParsedRow(int line, String code, VacancyRequest request) {}
}
