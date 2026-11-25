package ru.mirea.hrsystem.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.config.AppProperties;
import ru.mirea.hrsystem.domain.RefreshSession;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.dto.AuthDtos.AuthResponse;
import ru.mirea.hrsystem.dto.AuthDtos.ChangePasswordRequest;
import ru.mirea.hrsystem.dto.AuthDtos.LoginRequest;
import ru.mirea.hrsystem.dto.AuthDtos.MeResponse;
import ru.mirea.hrsystem.error.AuthenticationFailedException;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.repository.RefreshSessionRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.security.CurrentUserService;
import ru.mirea.hrsystem.security.JwtService;
import ru.mirea.hrsystem.security.PasswordPolicy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
public class AuthService {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final AuditService audit;
    private final CurrentUserService currentUser;
    private final PasswordPolicy passwordPolicy;
    private final AppProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginAttemptService loginAttempts, AuditService audit,
                       CurrentUserService currentUser, PasswordPolicy passwordPolicy,
                       AppProperties properties) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.audit = audit;
        this.currentUser = currentUser;
        this.passwordPolicy = passwordPolicy;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.email().trim().toLowerCase();
        if (loginAttempts.isBlocked(email)) {
            throw new AuthenticationFailedException("Слишком много попыток входа. Попробуйте позже");
        }
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttempts.loginFailed(email);
            audit.recordAnonymous(email, "LOGIN_FAILED", "Неудачная попытка входа");
            throw new AuthenticationFailedException("Неверный email или пароль");
        }
        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("Учётная запись отключена");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new AuthenticationFailedException("Учётная запись временно заблокирована");
        }
        loginAttempts.loginSucceeded(email);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        String refresh = issueRefresh(user, httpRequest, httpResponse);
        String access = jwtService.createAccessToken(user);
        audit.record("LOGIN", "USER", user.getUuid(), "Успешный вход", Map.of());
        return new AuthResponse(access, "Bearer", jwtService.accessTtlSeconds(),
                user.isMustChangePassword(), toMe(user));
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = readRefreshCookie(request);
        if (token == null) {
            throw new AuthenticationFailedException("Refresh-токен отсутствует");
        }
        RefreshSession session = sessions.findByTokenHash(hash(token))
                .orElseThrow(() -> new AuthenticationFailedException("Недействительный refresh-токен"));
        if (!session.isActive()) {
            throw new AuthenticationFailedException("Refresh-сессия отозвана или истекла");
        }
        UserAccount user = session.getUser();
        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("Учётная запись отключена");
        }
        session.setRevokedAt(Instant.now());
        issueRefresh(user, request, response);
        return new AuthResponse(jwtService.createAccessToken(user), "Bearer",
                jwtService.accessTtlSeconds(), user.isMustChangePassword(), toMe(user));
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = readRefreshCookie(request);
        if (token != null) {
            sessions.findByTokenHash(hash(token)).ifPresent(s -> s.setRevokedAt(Instant.now()));
        }
        clearRefreshCookie(response);
        try {
            UserAccount user = currentUser.requireEntity();
            audit.record("LOGOUT", "USER", user.getUuid(), "Выход из системы", Map.of());
        } catch (Exception ignored) {
        }
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        return toMe(currentUser.requireEntity());
    }

    @Transactional
    public MeResponse changePassword(ChangePasswordRequest request) {
        UserAccount user = currentUser.requireEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Текущий пароль неверен");
        }
        passwordPolicy.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        sessions.revokeAllForUser(user.getId(), Instant.now());
        audit.record("PASSWORD_CHANGED", "USER", user.getUuid(), "Пароль изменён", Map.of());
        return toMe(user);
    }

    private String issueRefresh(UserAccount user, HttpServletRequest request, HttpServletResponse response) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        Instant expires = Instant.now().plusSeconds(properties.jwt().refreshDays() * 24 * 3600);
        sessions.save(new RefreshSession(user, hash(raw), expires,
                request.getHeader("User-Agent"), request.getRemoteAddr()));
        Cookie cookie = new Cookie(REFRESH_COOKIE, raw);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (properties.jwt().refreshDays() * 24 * 3600));
        cookie.setSecure(properties.cookieSecure());
        response.addCookie(cookie);
        return raw;
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MeResponse toMe(UserAccount user) {
        return new MeResponse(user.getUuid(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isMustChangePassword());
    }
}
