package ru.mirea.hrsystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.error.BadRequestException;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Инварианты предметной области:
 * - archive не удаляет строку
 * - restore снимает archived
 * - salaryMax < salaryMin запрещён
 * - повторный archive идемпотентен
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VacancyInvariantsTest {
    @Autowired VacancyService vacancies;
    @Autowired EmployerRepository employers;
    @Autowired UserAccountRepository users;

    Employer employer;

    @BeforeEach
    void setUp() {
        UserAccount actor = new UserAccount(UUID.randomUUID(), "inv@example.org", "Inv", "{noop}x", Role.EDITOR);
        users.save(actor);
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        employer = new Employer(UUID.randomUUID(), "Inv Employer");
        employer.setCode("INV_EMP");
        employer = employers.saveAndFlush(employer);
    }

    @Test
    void archiveDoesNotDeleteAndIsIdempotent() {
        VacancyResponse created = vacancies.create(req("Title A", new BigDecimal("120000"), new BigDecimal("150000")));
        VacancyResponse archived = vacancies.archiveVacancy(created.uuid());
        assertTrue(archived.archived());
        VacancyResponse again = vacancies.archiveVacancy(created.uuid());
        assertTrue(again.archived());
        VacancyResponse restored = vacancies.restoreVacancy(created.uuid());
        assertFalse(restored.archived());
    }

    @Test
    void invalidSalaryRangeRejected() {
        assertThrows(BadRequestException.class, () ->
                vacancies.create(req("Bad", new BigDecimal("200000"), new BigDecimal("100000"))));
    }

    private VacancyRequest req(String title, BigDecimal min, BigDecimal max) {
        return new VacancyRequest(title, "d", employer.getUuid(), VacancyStatus.OPEN, min, max, false, Set.of("t"), null);
    }
}
