package ru.mirea.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.dto.ImportDtos.ImportMode;
import ru.mirea.hrsystem.dto.ImportDtos.ImportPreview;
import ru.mirea.hrsystem.dto.ImportDtos.ImportResult;
import ru.mirea.hrsystem.dto.PageResponse;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkResult;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.filter.VacancyFilter;
import ru.mirea.hrsystem.service.BulkOperationService;
import ru.mirea.hrsystem.service.CsvVacancyService;
import ru.mirea.hrsystem.service.VacancyService;
import ru.mirea.hrsystem.web.PageableFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/vacancies")
@SecurityRequirement(name = "bearerAuth")
public class VacancyController {
    private final VacancyService vacancies;
    private final CsvVacancyService csv;
    private final BulkOperationService bulk;
    private final PageableFactory pageableFactory;

    public VacancyController(VacancyService vacancies, CsvVacancyService csv,
                             BulkOperationService bulk, PageableFactory pageableFactory) {
        this.vacancies = vacancies;
        this.csv = csv;
        this.bulk = bulk;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public PageResponse<VacancyResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID employerUuid,
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) BigDecimal salaryFrom,
            @RequestParam(required = false) BigDecimal salaryTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        VacancyFilter filter = new VacancyFilter(q, employerUuid, status, remote, priority, tag, archived, salaryFrom, salaryTo);
        return PageResponse.from(vacancies.search(filter, pageableFactory.vacancies(page, size, sort)));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public VacancyResponse get(@PathVariable UUID uuid) {
        return vacancies.get(uuid);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<VacancyResponse> create(@Valid @RequestBody VacancyRequest request) {
        return ResponseEntity.status(201).body(vacancies.create(request));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public VacancyResponse update(@PathVariable UUID uuid, @Valid @RequestBody VacancyRequest request) {
        return vacancies.update(uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public VacancyResponse archive(@PathVariable UUID uuid) {
        return vacancies.archiveVacancy(uuid);
    }

    @PostMapping("/{uuid}/restore")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public VacancyResponse restore(@PathVariable UUID uuid) {
        return vacancies.restoreVacancy(uuid);
    }

    @DeleteMapping("/{uuid}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable UUID uuid,
            @RequestHeader(name = "X-Confirm-Delete", required = false) String confirmation) {
        vacancies.permanentDelete(uuid, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return bulk.execute(request);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public void export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID employerUuid,
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) BigDecimal salaryFrom,
            @RequestParam(required = false) BigDecimal salaryTo,
            HttpServletResponse response) throws IOException {
        VacancyFilter filter = new VacancyFilter(q, employerUuid, status, remote, priority, tag, archived, salaryFrom, salaryTo);
        byte[] data = csv.export(filter);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vacancies.csv");
        response.getOutputStream().write(data);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ImportPreview preview(@RequestPart("file") MultipartFile file,
                                 @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ImportResult apply(@RequestPart("file") MultipartFile file,
                              @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }
}
