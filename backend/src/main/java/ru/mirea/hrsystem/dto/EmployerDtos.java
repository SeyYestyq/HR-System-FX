package ru.mirea.hrsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EmployerDtos {
    private EmployerDtos() {}

    public record EmployerResponse(
            UUID uuid,
            String name,
            String code,
            String industry,
            String website,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public record EmployerRequest(
            @NotBlank @Size(min = 2, max = 150) String name,
            @Size(max = 50) String code,
            @Size(max = 100) String industry,
            @Size(max = 255) String website,
            Boolean enabled
    ) {}
}
