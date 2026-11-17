package ru.mirea.hrsystem.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.mirea.hrsystem.domain.Role;
import ru.mirea.hrsystem.domain.UserAccount;
import ru.mirea.hrsystem.repository.UserAccountRepository;
import ru.mirea.hrsystem.service.LoginAttemptService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityPolicyTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired LoginAttemptService loginAttempts;

    @BeforeEach
    void setUp() {
        loginAttempts.clear();
        users.deleteAll();
        UserAccount user = new UserAccount(UUID.randomUUID(), "editor@example.org", "Editor",
                passwordEncoder.encode("Secret123"), Role.EDITOR);
        users.save(user);
        UserAccount disabled = new UserAccount(UUID.randomUUID(), "disabled@example.org", "Disabled",
                passwordEncoder.encode("Secret123"), Role.VIEWER);
        disabled.setEnabled(false);
        users.save(disabled);
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"editor@example.org","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"disabled@example.org","password":"Secret123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsMustChangePasswordFlag() throws Exception {
        UserAccount user = users.findByEmailIgnoreCase("editor@example.org").orElseThrow();
        user.setMustChangePassword(true);
        users.save(user);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"editor@example.org","password":"Secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.get("accessToken").asText().length() > 20);
    }

    @Test
    void fiveFailuresBlockLogin() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"editor@example.org","password":"bad"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
        assertTrue(loginAttempts.isBlocked("editor@example.org"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"editor@example.org","password":"Secret123"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
