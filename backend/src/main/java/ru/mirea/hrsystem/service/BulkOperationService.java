package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkAction;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkFailure;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkResult;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.repository.VacancyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BulkOperationService {
    private final VacancyService vacancies;
    private final VacancyRepository vacancyRepository;
    private final AuditService audit;

    public BulkOperationService(VacancyService vacancies, VacancyRepository vacancyRepository, AuditService audit) {
        this.vacancies = vacancies;
        this.vacancyRepository = vacancyRepository;
        this.audit = audit;
    }

    @Transactional
    public BulkResult execute(BulkRequest request) {
        if (request.action() == BulkAction.CHANGE_STATUS && request.status() == null) {
            throw new BadRequestException("Для смены статуса нужен status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом нужен tag");
        }
        List<UUID> successful = new ArrayList<>();
        List<BulkFailure> failed = new ArrayList<>();
        for (UUID uuid : request.uuids()) {
            // Не вызываем require()/throw для отсутствующих UUID: иначе вся bulk-транзакция
            // помечается rollback-only (UnexpectedRollbackException).
            if (vacancyRepository.findByUuid(uuid).isEmpty()) {
                failed.add(new BulkFailure(uuid, "Вакансия не найдена"));
                continue;
            }
            try {
                switch (request.action()) {
                    case ARCHIVE -> vacancies.archiveVacancy(uuid);
                    case RESTORE -> vacancies.restoreVacancy(uuid);
                    case CHANGE_STATUS -> vacancies.changeStatus(uuid, request.status());
                    case ADD_TAG -> vacancies.addTag(uuid, request.tag());
                    case REMOVE_TAG -> vacancies.removeTag(uuid, request.tag());
                }
                successful.add(uuid);
            } catch (RuntimeException ex) {
                failed.add(new BulkFailure(uuid, ex.getMessage()));
            }
        }
        audit.record("VACANCY_BULK_" + request.action().name(), "VACANCY", null,
                "Массовая операция", Map.of("processed", request.uuids().size(), "succeeded", successful.size()));
        return new BulkResult(request.uuids().size(), successful, failed);
    }
}
