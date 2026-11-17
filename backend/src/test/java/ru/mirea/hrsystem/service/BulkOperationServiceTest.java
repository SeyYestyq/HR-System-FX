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
import ru.mirea.hrsystem.dto.VacancyDtos.BulkAction;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.BulkResult;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyRequest;
import ru.mirea.hrsystem.dto.VacancyDtos.VacancyResponse;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BulkOperationServiceTest {
    @Autowired BulkOperationService bulk;
    @Autowired VacancyService vacancyService;
    @Autowired EmployerRepository employers;
    @Autowired UserAccountRepository users;

    VacancyResponse vacancy;

    @BeforeEach
    void setUp() {
        UserAccount actor = new UserAccount(UUID.randomUUID(), "bulk@example.org", "Bulk",
                "{noop}x", Role.EDITOR);
        users.save(actor);
        UserPrincipal principal = new UserPrincipal(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Employer employer = new Employer(UUID.randomUUID(), "Озон");
        employer.setCode("OZON");
        employers.save(employer);
        vacancy = vacancyService.create(new VacancyRequest("Lead", "d", employer.getUuid(),
                VacancyStatus.OPEN, new BigDecimal("300000"), new BigDecimal("400000"),
                true, Set.of(), null));
    }

    @Test
    void mixedUuidsProduceSuccessAndFailure() {
        UUID missing = UUID.randomUUID();
        BulkResult result = bulk.execute(new BulkRequest(BulkAction.ARCHIVE,
                List.of(vacancy.uuid(), missing), null, null));
        assertEquals(2, result.processed());
        assertEquals(1, result.successful().size());
        assertEquals(1, result.failed().size());
        assertEquals(missing, result.failed().getFirst().uuid());
    }
}
