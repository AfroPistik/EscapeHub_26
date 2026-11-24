package com.team.team;

import com.team.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamCreationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Το IntegrationTestBase καθαρίζει μόνο ΠΡΙΝ από κάθε test. Εδώ καθαρίζουμε και ΜΕΤΑ,
    // ώστε οι χρήστες/ομάδες αυτών των tests να μη μένουν στη βάση και να μη χαλούν
    // άλλα tests που τρέχουν αργότερα (π.χ. TeamMemberRepositoryTest με username "alice").
    @AfterEach
    void cleanUpAfterTest() {
        jdbcTemplate.execute("TRUNCATE team_members, teams, users RESTART IDENTITY CASCADE");
    }

    private MockHttpSession loggedInUser() throws Exception {
        registerUser("alice@example.com", "alice", "password123");
        return login("alice@example.com", "password123");
    }

    @Test
    void createTeam_returnsIdNameAndEightCharInviteCode() throws Exception {
        MockHttpSession session = loggedInUser();

        String body = createTeam(session, "Escape Masters");

        Number id = field(body, "$.id");
        String name = field(body, "$.name");
        String inviteCode = field(body, "$.inviteCode");
        assertNotNull(id);
        assertEquals("Escape Masters", name);
        assertEquals(8, inviteCode.length());
    }

    @Test
    void createTeam_storesCreatorAsOwnerInDatabase() throws Exception {
        MockHttpSession session = loggedInUser();

        String body = createTeam(session, "Escape Masters");
        Number teamId = field(body, "$.id");

        Integer members = jdbcTemplate.queryForObject(
                "select count(*) from team_members where team_id = ?", Integer.class, teamId.longValue());
        String role = jdbcTemplate.queryForObject(
                "select role from team_members where team_id = ?", String.class, teamId.longValue());
        String username = jdbcTemplate.queryForObject(
                "select u.username from team_members tm join users u on u.id = tm.user_id where tm.team_id = ?",
                String.class, teamId.longValue());

        assertEquals(1, members);
        assertEquals("OWNER", role);
        assertEquals("alice", username);
    }

    static Stream<String> invalidNames() {
        return Stream.of("", "ab", "   ab   ", "a".repeat(51));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void createTeam_withInvalidName_returns400AndStoresNothing(String name) throws Exception {
        MockHttpSession session = loggedInUser();

        mvc.perform(post("/api/teams")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        Integer teams = jdbcTemplate.queryForObject("select count(*) from teams", Integer.class);
        assertEquals(0, teams);
    }

    @Test
    void createTeam_withoutLogin_returns401() throws Exception {
        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Escape Masters\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}