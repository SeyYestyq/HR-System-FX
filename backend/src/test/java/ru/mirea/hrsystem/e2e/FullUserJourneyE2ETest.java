package ru.mirea.hrsystem.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.repository.EmployerRepository;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.repository.VacancyRepository;
import ru.mirea.hrsystem.service.LoginAttemptService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E: полный пользовательский сценарий через HTTP API.
 * login → employer → vacancy CRUD → notes → filters → bulk → CSV → analytics → admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullUserJourneyE2ETest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;
    @Autowired UserAccountRepository users;
    @Autowired EmployerRepository employers;
    @Autowired VacancyRepository vacancies;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired LoginAttemptService loginAttempts;

    static String adminToken;
    static String editorToken;
    static String viewerToken;
    static String employerUuid;
    static String vacancyUuid;
    static long vacancyVersion;
    static String noteUuid;

    @BeforeEach
    void seedUsersOnce() {
        loginAttempts.clear();
        if (users.findByEmailIgnoreCase("admin-e2e@example.org").isEmpty()) {
            users.save(admin("admin-e2e@example.org", "Admin E2E", Role.ADMIN, "AdminPass1"));
            users.save(admin("editor-e2e@example.org", "Editor E2E", Role.EDITOR, "EditorPass1"));
            users.save(admin("viewer-e2e@example.org", "Viewer E2E", Role.VIEWER, "ViewerPass1"));
        }
    }

    private UserAccount admin(String email, String name, Role role, String password) {
        UserAccount u = new UserAccount(UUID.randomUUID(), email, name, passwordEncoder.encode(password), role);
        u.setMustChangePassword(false);
        u.setEnabled(true);
        return u;
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @Order(1)
    void step01_loginAllRoles() throws Exception {
        adminToken = login("admin-e2e@example.org", "AdminPass1");
        editorToken = login("editor-e2e@example.org", "EditorPass1");
        viewerToken = login("viewer-e2e@example.org", "ViewerPass1");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @Order(2)
    void step02_viewerCannotCreateEmployer_adminCan() throws Exception {
        mockMvc.perform(post("/api/employers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Viewer Corp","code":"VIEWCORP"}
                                """))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/employers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Яндекс E2E","code":"YANDEX_E2E","industry":"IT","enabled":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andReturn();
        employerUuid = om.readTree(created.getResponse().getContentAsString()).get("uuid").asText();
    }

    @Test
    @Order(3)
    void step03_editorCreatesAndUpdatesVacancy_versionConflict() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/vacancies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Java Backend E2E",
                                  "description":"Spring Boot API",
                                  "employerUuid":"%s",
                                  "status":"OPEN",
                                  "salaryMin":210000,
                                  "salaryMax":300000,
                                  "remote":true,
                                  "tags":["java","spring"]
                                }
                                """.formatted(employerUuid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.archived").value(false))
                .andReturn();
        JsonNode body = om.readTree(created.getResponse().getContentAsString());
        vacancyUuid = body.get("uuid").asText();
        vacancyVersion = body.get("version").asLong();

        mockMvc.perform(put("/api/vacancies/" + vacancyUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Java Backend E2E Updated",
                                  "description":"Spring Boot API",
                                  "employerUuid":"%s",
                                  "status":"OPEN",
                                  "salaryMin":210000,
                                  "salaryMax":300000,
                                  "remote":true,
                                  "tags":["java","spring"],
                                  "version":%d
                                }
                                """.formatted(employerUuid, vacancyVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Backend E2E Updated"));

        // stale version -> 409
        mockMvc.perform(put("/api/vacancies/" + vacancyUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Stale",
                                  "description":"x",
                                  "employerUuid":"%s",
                                  "status":"OPEN",
                                  "salaryMin":210000,
                                  "salaryMax":300000,
                                  "remote":true,
                                  "tags":["java"],
                                  "version":%d
                                }
                                """.formatted(employerUuid, vacancyVersion)))
                .andExpect(status().isConflict());

        MvcResult fresh = mockMvc.perform(get("/api/vacancies/" + vacancyUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andReturn();
        vacancyVersion = om.readTree(fresh.getResponse().getContentAsString()).get("version").asLong();
    }

    @Test
    @Order(4)
    void step04_notesAndFilters() throws Exception {
        MvcResult note = mockMvc.perform(post("/api/vacancies/" + vacancyUuid + "/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Срочный найм","pinned":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinned").value(true))
                .andReturn();
        noteUuid = om.readTree(note.getResponse().getContentAsString()).get("uuid").asText();

        mockMvc.perform(get("/api/vacancies/" + vacancyUuid + "/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Срочный найм"));

        mockMvc.perform(get("/api/vacancies")
                        .param("q", "Backend E2E")
                        .param("status", "OPEN")
                        .param("remote", "true")
                        .param("priority", "HIGH")
                        .param("tag", "java")
                        .param("archived", "false")
                        .param("salaryFrom", "200000")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "title,asc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        // page size capped
        mockMvc.perform(get("/api/vacancies")
                        .param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @Order(5)
    void step05_bulkArchiveRestore() throws Exception {
        mockMvc.perform(post("/api/vacancies/bulk")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"ARCHIVE","uuids":["%s","%s"]}
                                """.formatted(vacancyUuid, UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(2))
                .andExpect(jsonPath("$.successful", hasSize(1)))
                .andExpect(jsonPath("$.failed", hasSize(1)));

        mockMvc.perform(get("/api/vacancies/" + vacancyUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(post("/api/vacancies/bulk")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"RESTORE","uuids":["%s"]}
                                """.formatted(vacancyUuid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful", hasSize(1)));
    }

    @Test
    @Order(6)
    void step06_csvExportImportAndAnalytics() throws Exception {
        MvcResult export = mockMvc.perform(get("/api/vacancies/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andReturn();
        String csv = export.getResponse().getContentAsString();
        assertTrue(csv.contains("code"));
        assertTrue(csv.contains("Java Backend E2E"));

        String importCsv = """
                code,title,description,employerUuid,status,salaryMin,salaryMax,remote,tags
                VAC-E2E-IMP,Imported Vacancy,desc,%s,DRAFT,90000,110000,false,qa
                """.formatted(employerUuid);
        MockMultipartFile file = new MockMultipartFile("file", "import.csv",
                "text/csv", importCsv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/vacancies/import/preview").file(file).param("mode", "CREATE_ONLY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validRows").value(1));

        mockMvc.perform(multipart("/api/vacancies/import/apply").file(file).param("mode", "CREATE_ONLY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(1));

        mockMvc.perform(get("/api/analytics/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.byStatus").isArray())
                .andExpect(jsonPath("$.byPriority").isArray());
    }

    @Test
    @Order(7)
    void step07_adminUsersAuditAndRoleGuards() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/admin/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("q", "VACANCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // viewer cannot create vacancy
        mockMvc.perform(post("/api/vacancies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Should Fail",
                                  "employerUuid":"%s",
                                  "status":"DRAFT",
                                  "remote":false,
                                  "tags":[]
                                }
                                """.formatted(employerUuid)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void step08_archiveKeepsRow_permanentNeedsAdmin() throws Exception {
        mockMvc.perform(delete("/api/vacancies/" + vacancyUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        assertTrue(vacancies.findByUuid(UUID.fromString(vacancyUuid)).isPresent());

        mockMvc.perform(delete("/api/vacancies/" + vacancyUuid + "/permanent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                        .header("X-Confirm-Delete", "PERMANENTLY_DELETE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/vacancies/" + vacancyUuid + "/permanent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Confirm-Delete", "PERMANENTLY_DELETE"))
                .andExpect(status().isNoContent());

        assertFalse(vacancies.findByUuid(UUID.fromString(vacancyUuid)).isPresent());
        assertTrue(noteUuid != null);
    }
}
