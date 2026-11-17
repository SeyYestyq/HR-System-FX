package ru.mirea.hrsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.AuditEvent;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.dto.AuditDtos.AuditResponse;
import ru.mirea.hrsystem.repository.AuditEventRepository;
import ru.mirea.hrsystem.security.CurrentUserService;
import ru.mirea.hrsystem.web.RequestMetadata;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository events, CurrentUserService currentUser, ObjectMapper objectMapper) {
        this.events = events;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String eventType, String entityType, UUID entityUuid, String description, Map<String, ?> details) {
        UserAccount actor = null;
        String email = null;
        try {
            actor = currentUser.requireEntity();
            email = actor.getEmail();
        } catch (Exception ignored) {
        }
        String json = null;
        if (details != null && !details.isEmpty()) {
            try {
                json = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException ignored) {
            }
        }
        events.save(new AuditEvent(actor, email, eventType, entityType, entityUuid, description,
                RequestMetadata.correlationId(), json));
    }

    @Transactional
    public void recordAnonymous(String email, String eventType, String description) {
        events.save(new AuditEvent(null, email, eventType, null, null, description,
                RequestMetadata.correlationId(), null));
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> search(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return events.search(query, pageable).map(a -> new AuditResponse(
                a.getUuid(), a.getActorEmail(), a.getEventType(), a.getEntityType(),
                a.getEntityUuid(), a.getDescription(), a.getCorrelationId(), a.getCreatedAt()));
    }
}
