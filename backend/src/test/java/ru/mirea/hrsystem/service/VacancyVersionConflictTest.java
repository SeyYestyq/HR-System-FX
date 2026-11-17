package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.error.ConflictException;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VacancyVersionConflictTest {
    @Autowired VacancyService vacancyService;
    @Autowired EmployerRepository employers;
    @Autowired UserAccountRepository users;

    Employer employer;

    @BeforeEach
    void setUp() {
        UserAccount actor = new UserAccount(UUID.randomUUID(), "actor@example.org", "Actor",
                "{noop}x", Role.EDITOR);
        users.save(actor);
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        employer = new Employer(UUID.randomUUID(), "Сбер");
        employer.setCode("SBER");
        employers.save(employer);
    }

    @Test
    void secondUpdateWithStaleVersionFails() {
        VacancyRequest create = new VacancyRequest("Java Dev", "desc", employer.getUuid(),
                VacancyStatus.OPEN, new BigDecimal("100000"), new BigDecimal("150000"),
                false, Set.of("java"), null);
        VacancyResponse created = vacancyService.create(create);
        Long initialVersion = created.version();

        VacancyRequest update1 = new VacancyRequest("Java Dev Updated", "desc", employer.getUuid(),
                VacancyStatus.OPEN, new BigDecimal("100000"), new BigDecimal("150000"),
                false, Set.of("java"), initialVersion);
        vacancyService.update(created.uuid(), update1);

        VacancyRequest stale = new VacancyRequest("Stale", "desc", employer.getUuid(),
                VacancyStatus.OPEN, new BigDecimal("100000"), new BigDecimal("150000"),
                false, Set.of("java"), initialVersion);
        assertThrows(ConflictException.class, () -> vacancyService.update(created.uuid(), stale));
    }
}
