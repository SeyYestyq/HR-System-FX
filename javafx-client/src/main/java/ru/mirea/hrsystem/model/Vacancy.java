package ru.mirea.hrsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Vacancy {
    private Long id;
    private String title;
    private String description;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private VacancyStatus status;
    private Long employerId;
    private String employerCompanyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Vacancy() {
        this.status = VacancyStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Vacancy(Long id, String title, String description, BigDecimal salaryMin,
                   BigDecimal salaryMax, VacancyStatus status, Long employerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.status = status != null ? status : VacancyStatus.ACTIVE;
        this.employerId = employerId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }

    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }

    public VacancyStatus getStatus() { return status; }
    public void setStatus(VacancyStatus status) { this.status = status; }

    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }

    public String getEmployerCompanyName() { return employerCompanyName; }
    public void setEmployerCompanyName(String employerCompanyName) { this.employerCompanyName = employerCompanyName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Vacancy{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", employerId=" + employerId +
                '}';
    }
}
