package ru.mirea.hrsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.hrsystem.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    @Query("""
        select a from AuditEvent a
        where (:q is null or lower(a.description) like lower(concat('%', cast(:q as string), '%'))
               or lower(a.eventType) like lower(concat('%', cast(:q as string), '%'))
               or lower(a.actorEmail) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<AuditEvent> search(@Param("q") String q, Pageable pageable);
}
