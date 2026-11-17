package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.domain.VacancyNote;
import ru.mirea.hrsystem.dto.NoteDtos.NoteRequest;
import ru.mirea.hrsystem.dto.NoteDtos.NoteResponse;
import ru.mirea.hrsystem.error.ConflictException;
import ru.mirea.hrsystem.error.NotFoundException;
import ru.mirea.hrsystem.repository.VacancyNoteRepository;
import ru.mirea.hrsystem.security.CurrentUserService;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {
    private final VacancyNoteRepository notes;
    private final VacancyService vacancies;
    private final CurrentUserService currentUser;

    public NoteService(VacancyNoteRepository notes, VacancyService vacancies, CurrentUserService currentUser) {
        this.notes = notes;
        this.vacancies = vacancies;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID vacancyUuid) {
        vacancies.require(vacancyUuid);
        return notes.findByVacancyUuidOrderByPinnedDescCreatedAtDesc(vacancyUuid).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID vacancyUuid, NoteRequest request) {
        Vacancy vacancy = vacancies.require(vacancyUuid);
        UserAccount author = currentUser.requireEntity();
        VacancyNote note = new VacancyNote(UUID.randomUUID(), vacancy, author, request.content().trim());
        note.setPinned(request.pinned());
        notes.save(note);
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID noteUuid, NoteRequest request) {
        VacancyNote note = notes.findByUuid(noteUuid)
                .orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (request.version() != null && !request.version().equals(note.getVersion())) {
            throw new ConflictException("Конфликт версий заметки");
        }
        note.setContent(request.content().trim());
        note.setPinned(request.pinned());
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID noteUuid) {
        VacancyNote note = notes.findByUuid(noteUuid)
                .orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        notes.delete(note);
    }

    private NoteResponse toResponse(VacancyNote note) {
        return new NoteResponse(note.getUuid(), note.getContent(), note.isPinned(),
                note.getAuthor().getUuid(), note.getAuthor().getFullName(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
