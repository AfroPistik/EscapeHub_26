package com.team;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
// Το @AutoConfigureMockMvc: βάλε το import με Alt+Enter (στο Spring Boot 4 άλλαξε πακέτο)
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MainTests.TestCurrentUserConfig.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    // Καθαρισμός ανάμεσα στα tests. Όχι @Transactional: τα tests ταυτόχρονων
    // ενεργειών χρειάζονται πραγματικά commits.
    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE team_members, teams, users RESTART IDENTITY CASCADE");
    }

    protected void registerUser(String email, String username, String password) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"%s","username":"%s","password":"%s"}
                    """.formatted(email, username, password)))
                .andExpect(status().isCreated());
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Επιστρέφει το JSON της απάντησης. Διάβασε πεδία με field(body, "$.inviteCode"). */
    protected String createTeam(MockHttpSession session, String name) throws Exception {
        return mvc.perform(post("/api/teams")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"%s"}
                    """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    protected void joinTeam(MockHttpSession session, String inviteCode) throws Exception {
        mvc.perform(post("/api/teams/join")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"inviteCode":"%s"}
                    """.formatted(inviteCode)))
                .andExpect(status().isOk());
    }

    protected <T> T field(String json, String jsonPath) {
        return JsonPath.read(json, jsonPath);
    }
}