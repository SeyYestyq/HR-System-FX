package ru.mirea.hrsystem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.hrsystem.dto.NoteDtos.NoteRequest;
import ru.mirea.hrsystem.dto.NoteDtos.NoteResponse;
import ru.mirea.hrsystem.service.NoteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vacancies/{vacancyUuid}/notes")
@SecurityRequirement(name = "bearerAuth")
public class VacancyNoteController {
    private final NoteService notes;

    public VacancyNoteController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public List<NoteResponse> list(@PathVariable UUID vacancyUuid) {
        return notes.list(vacancyUuid);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<NoteResponse> create(@PathVariable UUID vacancyUuid,
                                               @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(vacancyUuid, request));
    }

    @PutMapping("/{noteUuid}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public NoteResponse update(@PathVariable UUID vacancyUuid,
                               @PathVariable UUID noteUuid,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(noteUuid, request);
    }

    @DeleteMapping("/{noteUuid}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID vacancyUuid, @PathVariable UUID noteUuid) {
        notes.delete(noteUuid);
        return ResponseEntity.noContent().build();
    }
}
