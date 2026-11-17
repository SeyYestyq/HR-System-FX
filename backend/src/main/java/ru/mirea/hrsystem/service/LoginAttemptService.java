package ru.mirea.hrsystem.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 15 * 60;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Attempt attempt = attempts.get(normalize(email));
        return attempt != null && attempt.lockedUntil != null && attempt.lockedUntil.isAfter(Instant.now());
    }

    public Instant lockedUntil(String email) {
        Attempt attempt = attempts.get(normalize(email));
        return attempt == null ? null : attempt.lockedUntil;
    }

    public void loginSucceeded(String email) {
        attempts.remove(normalize(email));
    }

    /** Сброс состояния (для тестов). */
    public void clear() {
        attempts.clear();
    }

    public void loginFailed(String email) {
        String key = normalize(email);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plusSeconds(LOCK_SECONDS);
            attempt.count = 0;
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class Attempt {
        int count;
        Instant lockedUntil;
    }
}
