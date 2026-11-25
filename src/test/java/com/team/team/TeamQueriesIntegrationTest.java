package com.team.team;

import com.team.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamQueriesIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Καθαρίζουμε τη βάση και ΜΕΤΑ από κάθε test, για να μη μένουν δεδομένα σε άλλα tests.
    @AfterEach
    void cleanUpAfterTest() {
        jdbcTemplate.execute("TRUNCATE team_members, teams, users RESTART IDENTITY CASCADE");
    }

    private MockHttpSession loginAs(String name) throws Exception {
        registerUser(name + "@example.com", name, "password123");
        return login(name + "@example.com", "password123");
    }

    // Το POST /api/teams/join δεν υπάρχει ακόμα (US-11), οπότε προσθέτουμε το μέλος απευθείας στη βάση.
    private void addMember(long teamId, String username) {
        jdbcTemplate.update(
                "insert into team_members (team_id, user_id, role) "
                        + "values (?, (select id from users where username = ?), 'MEMBER')",
                teamId, username);
    }

    // ---------- 401 ----------

    @Test
    void getMyTeams_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/teams/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getTeam_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/teams/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ---------- GET /api/teams/{id} ----------

    @Test
    void getTeam_asOwner_returns200WithInviteCode() throws Exception {
        MockHttpSession alice = loginAs("alice");
        String body = createTeam(alice, "Alpha Team");
        Number id = field(body, "$.id");
        String inviteCode = field(body, "$.inviteCode");

        mvc.perform(get("/api/teams/" + id.longValue()).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.name").value("Alpha Team"))
                .andExpect(jsonPath("$.inviteCode").value(inviteCode));
    }

    @Test
    void getTeam_asJoinedMember_returns200WithInviteCode() throws Exception {
        MockHttpSession alice = loginAs("alice");
        MockHttpSession bob = loginAs("bob");
        String body = createTeam(alice, "Alpha Team");
        Number id = field(body, "$.id");
        String inviteCode = field(body, "$.inviteCode");
        addMember(id.longValue(), "bob");

        mvc.perform(get("/api/teams/" + id.longValue()).session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha Team"))
                .andExpect(jsonPath("$.inviteCode").value(inviteCode));
    }

    @Test
    void getTeam_asNonMember_returns403AndDoesNotLeakInviteCode() throws Exception {
        MockHttpSession alice = loginAs("alice");
        MockHttpSession bob = loginAs("bob");
        String body = createTeam(alice, "Alpha Team");
        Number id = field(body, "$.id");
        String inviteCode = field(body, "$.inviteCode");

        String response = mvc.perform(get("/api/teams/" + id.longValue()).session(bob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_A_MEMBER"))
                .andExpect(jsonPath("$.inviteCode").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains(inviteCode), "Το inviteCode δεν πρέπει να εμφανίζεται σε μη μέλος");
    }

    @Test
    void getTeam_whenTeamDoesNotExist_returns404() throws Exception {
        MockHttpSession alice = loginAs("alice");

        mvc.perform(get("/api/teams/9999").session(alice))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
    }

    // ---------- GET /api/teams/mine ----------

    @Test
    void getMyTeams_whenUserHasNoTeams_returnsEmptyList() throws Exception {
        MockHttpSession alice = loginAs("alice");

        mvc.perform(get("/api/teams/mine").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyTeams_returnsOnlyTeamsOfTheLoggedInUser() throws Exception {
        MockHttpSession alice = loginAs("alice");
        MockHttpSession bob = loginAs("bob");
        createTeam(alice, "Alpha Team");
        createTeam(alice, "Beta Team");
        createTeam(bob, "Gamma Team");

        // Η alice βλέπει μόνο τις 2 δικές της ομάδες (ταξινομημένες κατά όνομα).
        mvc.perform(get("/api/teams/mine").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha Team"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].name").value("Beta Team"))
                .andExpect(jsonPath("$[1].role").value("OWNER"));

        // Ο bob βλέπει μόνο τη δική του.
        mvc.perform(get("/api/teams/mine").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Gamma Team"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void getMyTeams_showsMemberRoleForJoinedTeam() throws Exception {
        MockHttpSession alice = loginAs("alice");
        MockHttpSession bob = loginAs("bob");
        String alphaBody = createTeam(alice, "Alpha Team");
        createTeam(bob, "Gamma Team");
        Number alphaId = field(alphaBody, "$.id");
        addMember(alphaId.longValue(), "bob");

        mvc.perform(get("/api/teams/mine").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha Team"))
                .andExpect(jsonPath("$[0].role").value("MEMBER"))
                .andExpect(jsonPath("$[1].name").value("Gamma Team"))
                .andExpect(jsonPath("$[1].role").value("OWNER"));
    }
}