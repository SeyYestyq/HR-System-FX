package ru.mirea.hrsystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.hrsystem.domain.Priority;
import ru.mirea.hrsystem.domain.VacancyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VacancyDtos {
    private VacancyDtos() {}

    public record VacancyResponse(
            UUID uuid,
            String code,
            String title,
            String description,
            UUID employerUuid,
            String employerName,
            VacancyStatus status,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            boolean remote,
            Set<String> tags,
            Priority priority,
            boolean archived,
            Instant archivedAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public record VacancyRequest(
            @NotBlank @Size(min = 3, max = 150) String title,
            @Size(max = 10000) String description,
            @NotNull UUID employerUuid,
            @NotNull VacancyStatus status,
            @DecimalMin("0.0") BigDecimal salaryMin,
            @DecimalMin("0.0") BigDecimal salaryMax,
            boolean remote,
            Set<String> tags,
            Long version
    ) {}

    public enum BulkAction {
        ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG
    }

    public record BulkRequest(
            @NotNull BulkAction action,
            @NotNull @Size(min = 1) List<UUID> uuids,
            VacancyStatus status,
            String tag
    ) {}

    public record BulkFailure(UUID uuid, String error) {}

    public record BulkResult(int processed, List<UUID> successful, List<BulkFailure> failed) {}
}
