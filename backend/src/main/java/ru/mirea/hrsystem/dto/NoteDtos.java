package ru.mirea.hrsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NoteDtos {
    private NoteDtos() {}

    public record NoteResponse(
            UUID uuid,
            String content,
            boolean pinned,
            UUID authorUuid,
            String authorName,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public record NoteRequest(
            @NotBlank @Size(max = 4000) String content,
            boolean pinned,
            Long version
    ) {}
}
