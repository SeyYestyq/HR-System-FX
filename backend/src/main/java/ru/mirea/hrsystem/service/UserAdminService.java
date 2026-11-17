package ru.mirea.hrsystem.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.dto.UserDtos.CreateUserRequest;
import ru.mirea.hrsystem.dto.UserDtos.CreatedUserResponse;
import ru.mirea.hrsystem.dto.UserDtos.UpdateUserRequest;
import ru.mirea.hrsystem.dto.UserDtos.UserResponse;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.error.NotFoundException;
import ru.mirea.hrsystem.repository.RefreshSessionRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.security.PasswordPolicy;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAdminService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public UserAdminService(UserAccountRepository users, RefreshSessionRepository sessions,
                            PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                            AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public CreatedUserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email уже используется");
        }
        String temporary = generatePassword();
        passwordPolicy.validate(temporary);
        UserAccount user = new UserAccount(UUID.randomUUID(), email, request.fullName().trim(),
                passwordEncoder.encode(temporary), request.role());
        user.setMustChangePassword(true);
        users.save(user);
        audit.record("USER_CREATED", "USER", user.getUuid(), "Создан пользователь " + email, Map.of());
        return new CreatedUserResponse(toResponse(user), temporary);
    }

    @Transactional
    public UserResponse update(UUID uuid, UpdateUserRequest request) {
        UserAccount user = require(uuid);
        String email = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email).ifPresent(other -> {
            if (!other.getId().equals(user.getId())) {
                throw new BadRequestException("Email уже используется");
            }
        });
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        if (!request.enabled()) {
            sessions.revokeAllForUser(user.getId(), Instant.now());
        }
        audit.record("USER_UPDATED", "USER", uuid, "Изменён пользователь " + email, Map.of());
        return toResponse(user);
    }

    @Transactional
    public CreatedUserResponse resetPassword(UUID uuid) {
        UserAccount user = require(uuid);
        String temporary = generatePassword();
        passwordPolicy.validate(temporary);
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        sessions.revokeAllForUser(user.getId(), Instant.now());
        audit.record("USER_PASSWORD_RESET", "USER", uuid, "Сброшен пароль пользователя", Map.of());
        return new CreatedUserResponse(toResponse(user), temporary);
    }

    private UserAccount require(UUID uuid) {
        return users.findByUuid(uuid).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private String generatePassword() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return "Tmp1!" + HexFormat.of().formatHex(bytes);
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getUuid(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isEnabled(), user.isMustChangePassword(), user.getCreatedAt());
    }
}
