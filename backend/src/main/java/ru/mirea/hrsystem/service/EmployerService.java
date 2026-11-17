package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.dto.EmployerDtos.EmployerRequest;
import ru.mirea.hrsystem.dto.EmployerDtos.EmployerResponse;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.error.NotFoundException;
import ru.mirea.hrsystem.repository.EmployerRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class EmployerService {
    private final EmployerRepository employers;
    private final AuditService audit;

    public EmployerService(EmployerRepository employers, AuditService audit) {
        this.employers = employers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<EmployerResponse> list(boolean onlyEnabled) {
        List<Employer> list = onlyEnabled
                ? employers.findByEnabledTrueOrderByNameAsc()
                : employers.findAllByOrderByNameAsc();
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployerResponse create(EmployerRequest request) {
        String code = normalizeCode(request.code(), request.name());
        if (employers.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Код работодателя уже используется");
        }
        Employer employer = new Employer(UUID.randomUUID(), request.name().trim());
        employer.setCode(code);
        employer.setIndustry(trim(request.industry()));
        employer.setWebsite(trim(request.website()));
        employer.setEnabled(request.enabled() == null || request.enabled());
        employers.save(employer);
        audit.record("EMPLOYER_CREATED", "EMPLOYER", employer.getUuid(),
                "Создан работодатель " + employer.getName(), Map.of());
        return toResponse(employer);
    }

    @Transactional
    public EmployerResponse update(UUID uuid, EmployerRequest request) {
        Employer employer = employers.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Работодатель не найден"));
        String code = normalizeCode(request.code(), request.name());
        employers.findByCodeIgnoreCase(code).ifPresent(other -> {
            if (!other.getId().equals(employer.getId())) {
                throw new BadRequestException("Код работодателя уже используется");
            }
        });
        employer.setName(request.name().trim());
        employer.setCode(code);
        employer.setIndustry(trim(request.industry()));
        employer.setWebsite(trim(request.website()));
        if (request.enabled() != null) {
            employer.setEnabled(request.enabled());
        }
        audit.record("EMPLOYER_UPDATED", "EMPLOYER", uuid,
                "Изменён работодатель " + employer.getName(), Map.of());
        return toResponse(employer);
    }

    private String normalizeCode(String code, String name) {
        if (code != null && !code.isBlank()) {
            return code.trim().toUpperCase(Locale.ROOT);
        }
        return name.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-ZА-Я0-9]+", "_");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EmployerResponse toResponse(Employer e) {
        return new EmployerResponse(e.getUuid(), e.getName(), e.getCode(), e.getIndustry(),
                e.getWebsite(), e.isEnabled(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
