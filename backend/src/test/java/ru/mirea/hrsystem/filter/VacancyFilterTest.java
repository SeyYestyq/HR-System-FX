package ru.mirea.hrsystem.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.hrsystem.domain.Employer;
import ru.mirea.hrsystem.domain.Vacancy;
import ru.mirea.hrsystem.domain.VacancyStatus;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.VacancyRepository;
import ru.mirea.hrsystem.web.PageableFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VacancyFilterTest {
    @Autowired VacancyRepository vacancies;
    @Autowired EmployerRepository employers;
    @Autowired PageableFactory pageableFactory;

    Employer employer;

    @BeforeEach
    void setUp() {
        vacancies.deleteAll();
        employers.deleteAll();
        Employer createdEmployer = new Employer(UUID.randomUUID(), "Яндекс");
        createdEmployer.setCode("YANDEX");
        employer = employers.saveAndFlush(createdEmployer);

        Vacancy open = new Vacancy(UUID.randomUUID(), "VAC-2026-0001", "Java Backend", employer);
        open.setStatus(VacancyStatus.OPEN);
        open.setSalaryMin(new BigDecimal("180000"));
        open.setSalaryMax(new BigDecimal("250000"));
        open.setRemote(true);
        open.getTags().add("java");
        vacancies.save(open);

        Vacancy draft = new Vacancy(UUID.randomUUID(), "VAC-2026-0002", "QA Engineer", employer);
        draft.setStatus(VacancyStatus.DRAFT);
        draft.setSalaryMin(new BigDecimal("80000"));
        draft.setRemote(false);
        vacancies.save(draft);
    }

    @Test
    void filtersByStatusAndRemote() {
        var filter = new VacancyFilter(null, null, VacancyStatus.OPEN, true, null, null, false, null, null);
        var page = vacancies.findAll(VacancySpecifications.from(filter), PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals("Java Backend", page.getContent().getFirst().getTitle());
    }

    @Test
    void pageSizeIsCappedAt100() {
        var pageable = pageableFactory.vacancies(0, 500, "title,asc");
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    void textSearchFindsByCode() {
        var filter = new VacancyFilter("vac-2026-0002", null, null, null, null, null, null, null, null);
        var page = vacancies.findAll(VacancySpecifications.from(filter), PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().getFirst().getTitle().contains("QA"));
    }
}
