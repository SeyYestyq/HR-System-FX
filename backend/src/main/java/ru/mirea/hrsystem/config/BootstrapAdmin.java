package ru.mirea.hrsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.repository.UserAccountRepository;

import java.util.UUID;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final AppProperties properties;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdmin(AppProperties properties, UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.bootstrap().email();
        String password = properties.bootstrap().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        users.findByEmailIgnoreCase(email).ifPresentOrElse(existing -> {
            if (!existing.isEnabled() || existing.getRole() != Role.ADMIN) {
                existing.setEnabled(true);
                existing.setRole(Role.ADMIN);
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setMustChangePassword(true);
                users.save(existing);
                log.info("Bootstrap admin updated: {}", email);
            }
        }, () -> {
            UserAccount admin = new UserAccount(UUID.randomUUID(), email.toLowerCase(),
                    properties.bootstrap().name(), passwordEncoder.encode(password), Role.ADMIN);
            admin.setMustChangePassword(true);
            users.save(admin);
            log.info("Bootstrap admin created: {}", email);
        });
    }
}
