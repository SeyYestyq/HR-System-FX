package ru.mirea.hrsystem.dto;

import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {}

    public record AuditResponse(
            UUID uuid,
            String actorEmail,
            String eventType,
            String entityType,
            UUID entityUuid,
            String description,
            String correlationId,
            Instant createdAt
    ) {}
}
