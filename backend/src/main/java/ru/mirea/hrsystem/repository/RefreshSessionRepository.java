package ru.mirea.hrsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.hrsystem.domain.RefreshSession;

import java.time.Instant;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllForUser(Long userId, Instant now);
}
