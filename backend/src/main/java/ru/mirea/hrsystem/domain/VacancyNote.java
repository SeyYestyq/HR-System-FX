package ru.mirea.hrsystem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "vacancy_notes")
public class VacancyNote extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private Vacancy vacancy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private boolean pinned;

    protected VacancyNote() {}

    public VacancyNote(UUID uuid, Vacancy vacancy, UserAccount author, String content) {
        this.uuid = uuid;
        this.vacancy = vacancy;
        this.author = author;
        this.content = content;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public Vacancy getVacancy() { return vacancy; }
    public UserAccount getAuthor() { return author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
