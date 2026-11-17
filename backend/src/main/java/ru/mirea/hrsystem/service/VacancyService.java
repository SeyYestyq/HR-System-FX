package ru.mirea.hrsystem.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.config.AppProperties;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.error.ConflictException;
import ru.mirea.hrsystem.error.NotFoundException;
import ru.mirea.hrsystem.filter.VacancyFilter;
import ru.mirea.hrsystem.filter.VacancySpecifications;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.VacancyNoteRepository;
import ru.mirea.hrsystem.repository.VacancyRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VacancyService {
    private final VacancyRepository vacancies;
    private final EmployerRepository employers;
    private final VacancyNoteRepository notes;
    private final VacancyMapper mapper;
    private final AuditService audit;
    private final AppProperties properties;
    private final AtomicLong codeSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    public VacancyService(VacancyRepository vacancies, EmployerRepository employers,
                          VacancyNoteRepository notes, VacancyMapper mapper,
                          AuditService audit, AppProperties properties) {
        this.vacancies = vacancies;
        this.employers = employers;
        this.notes = notes;
        this.mapper = mapper;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Page<VacancyResponse> search(VacancyFilter filter, Pageable pageable) {
        return vacancies.findAll(VacancySpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VacancyResponse get(UUID uuid) {
        return mapper.toResponse(require(uuid));
    }

    @Transactional
    public VacancyResponse create(VacancyRequest request) {
        validateSalary(request.salaryMin(), request.salaryMax());
        Employer employer = requireEmployer(request.employerUuid());
        Vacancy vacancy = new Vacancy(UUID.randomUUID(), nextCode(), normalize(request.title()), employer);
        apply(vacancy, request, employer);
        vacancies.saveAndFlush(vacancy);
        audit.record("VACANCY_CREATED", "VACANCY", vacancy.getUuid(),
                "Создана вакансия " + vacancy.getTitle(), Map.of());
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse update(UUID uuid, VacancyRequest request) {
        Vacancy vacancy = require(uuid);
        verifyVersion(request.version(), vacancy.getVersion());
        validateSalary(request.salaryMin(), request.salaryMax());
        Employer employer = requireEmployer(request.employerUuid());
        apply(vacancy, request, employer);
        audit.record("VACANCY_UPDATED", "VACANCY", uuid,
                "Изменена вакансия " + vacancy.getTitle(), Map.of("version", vacancy.getVersion()));
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse archiveVacancy(UUID uuid) {
        Vacancy vacancy = require(uuid);
        if (vacancy.getArchivedAt() == null) {
            vacancy.setArchivedAt(Instant.now());
        }
        audit.record("VACANCY_ARCHIVED", "VACANCY", uuid, "Вакансия архивирована", Map.of());
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse restoreVacancy(UUID uuid) {
        Vacancy vacancy = require(uuid);
        vacancy.setArchivedAt(null);
        audit.record("VACANCY_RESTORED", "VACANCY", uuid, "Вакансия восстановлена", Map.of());
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public void permanentDelete(UUID uuid, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        Vacancy vacancy = require(uuid);
        notes.deleteByVacancyId(vacancy.getId());
        vacancies.delete(vacancy);
        audit.record("VACANCY_PERMANENTLY_DELETED", "VACANCY", uuid, "Вакансия удалена безвозвратно", Map.of());
    }

    @Transactional
    public VacancyResponse changeStatus(UUID uuid, VacancyStatus status) {
        if (status == null) throw new BadRequestException("Статус обязателен");
        Vacancy vacancy = require(uuid);
        vacancy.setStatus(status);
        audit.record("VACANCY_STATUS_CHANGED", "VACANCY", uuid, "Статус изменён на " + status, Map.of());
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse addTag(UUID uuid, String tag) {
        String normalized = normalizeTag(tag);
        Vacancy vacancy = require(uuid);
        vacancy.getTags().add(normalized);
        return mapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse removeTag(UUID uuid, String tag) {
        String normalized = normalizeTag(tag);
        Vacancy vacancy = require(uuid);
        vacancy.getTags().remove(normalized);
        return mapper.toResponse(vacancy);
    }

    Vacancy upsertFromImport(Vacancy existing, VacancyRequest request, String code) {
        if (existing == null) {
            Employer employer = requireEmployer(request.employerUuid());
            Vacancy created = new Vacancy(UUID.randomUUID(),
                    code == null || code.isBlank() ? nextCode() : code.trim().toUpperCase(Locale.ROOT),
                    normalize(request.title()), employer);
            apply(created, request, employer);
            return vacancies.save(created);
        }
        Employer employer = requireEmployer(request.employerUuid());
        apply(existing, request, employer);
        return existing;
    }

    Vacancy require(UUID uuid) {
        return vacancies.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена"));
    }

    Vacancy findByCode(String code) {
        return vacancies.findByCodeIgnoreCase(code).orElse(null);
    }

    private void apply(Vacancy vacancy, VacancyRequest request, Employer employer) {
        vacancy.setTitle(normalize(request.title()));
        vacancy.setDescription(request.description() == null ? null : request.description().trim());
        vacancy.setEmployer(employer);
        vacancy.setStatus(request.status());
        vacancy.setSalaryMin(request.salaryMin());
        vacancy.setSalaryMax(request.salaryMax());
        vacancy.setRemote(request.remote());
        vacancy.setTags(normalizeTags(request.tags()));
    }

    private Employer requireEmployer(UUID uuid) {
        Employer employer = employers.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Работодатель не найден"));
        if (!employer.isEnabled()) {
            throw new BadRequestException("Работодатель отключён");
        }
        return employer;
    }

    private void validateSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BadRequestException("Максимальная зарплата не может быть меньше минимальной");
        }
    }

    private void verifyVersion(Long requestVersion, Long current) {
        if (requestVersion == null || !requestVersion.equals(current)) {
            throw new ConflictException("Конфликт версий: запись изменена другим пользователем");
        }
    }

    private String nextCode() {
        return "VAC-" + LocalDate.now().getYear() + "-" + String.format("%04d", codeSeq.incrementAndGet() % 10000);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    result.add(normalizeTag(tag));
                }
            }
        }
        return result;
    }

    private String normalizeTag(String tag) {
        if (tag == null || tag.isBlank()) throw new BadRequestException("Тег обязателен");
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}
