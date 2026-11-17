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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_uuid")
    private UUID entityUuid;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    protected AuditEvent() {}

    public AuditEvent(UserAccount user, String actorEmail, String eventType, String entityType,
                      UUID entityUuid, String description, String correlationId, String detailsJson) {
        this.user = user;
        this.actorEmail = actorEmail;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityUuid = entityUuid;
        this.description = description;
        this.correlationId = correlationId;
        this.detailsJson = detailsJson;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public UserAccount getUser() { return user; }
    public String getActorEmail() { return actorEmail; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityUuid() { return entityUuid; }
    public String getDescription() { return description; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getDetailsJson() { return detailsJson; }
}
