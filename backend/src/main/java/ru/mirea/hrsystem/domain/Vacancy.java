package ru.mirea.hrsystem.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "vacancies")
public class Vacancy extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VacancyStatus status = VacancyStatus.DRAFT;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "is_remote", nullable = false)
    private boolean remote;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vacancy_tags", joinColumns = @JoinColumn(name = "vacancy_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Vacancy() {}

    public Vacancy(UUID uuid, String code, String title, Employer employer) {
        this.uuid = uuid;
        this.code = code;
        this.title = title;
        this.employer = employer;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Employer getEmployer() { return employer; }
    public void setEmployer(Employer employer) { this.employer = employer; }
    public VacancyStatus getStatus() { return status; }
    public void setStatus(VacancyStatus status) { this.status = status; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean remote) { this.remote = remote; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public boolean isArchived() { return archivedAt != null; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}
