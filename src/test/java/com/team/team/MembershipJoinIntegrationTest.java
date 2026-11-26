
package com.team.team;

import com.team.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipJoinIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUpAfterTest() {
        jdbcTemplate.execute("TRUNCATE team_members, teams, users RESTART IDENTITY CASCADE");
    }

    @Test
    void joinTeam_withValidInviteCode_returns200AndAddsMember() throws Exception {
        registerUser("alice@example.com", "alice", "password123");
        MockHttpSession ownerSession = login("alice@example.com", "password123");
        String teamBody = createTeam(ownerSession, "Escape Masters");
        Number teamId = field(teamBody, "$.id");
        String inviteCode = field(teamBody, "$.inviteCode");

        registerUser("bob@example.com", "bob", "password123");
        MockHttpSession memberSession = login("bob@example.com", "password123");

        mvc.perform(post("/api/teams/join")
                        .session(memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"%s\"}".formatted(inviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teamId.longValue()))
                .andExpect(jsonPath("$.name").value("Escape Masters"))
                .andExpect(jsonPath("$.inviteCode").value(inviteCode));

        Integer membersCount = jdbcTemplate.queryForObject(
                "select count(*) from team_members where team_id = ?", Integer.class, teamId.longValue());
        String bobRole = jdbcTemplate.queryForObject(
                "select tm.role from team_members tm join users u on u.id = tm.user_id where tm.team_id = ? and u.username = ?",
                String.class, teamId.longValue(), "bob");

        assertEquals(2, membersCount);
        assertEquals("MEMBER", bobRole);
    }

    @Test
    void joinTeam_withInvalidInviteCode_returns404() throws Exception {
        registerUser("alice@example.com", "alice", "password123");
        MockHttpSession session = login("alice@example.com", "password123");

        mvc.perform(post("/api/teams/join")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"INVALID1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_INVITE_CODE"));
    }

    @Test
    void joinTeam_whenAlreadyMember_returns409() throws Exception {
        registerUser("alice@example.com", "alice", "password123");
        MockHttpSession ownerSession = login("alice@example.com", "password123");
        String teamBody = createTeam(ownerSession, "Escape Masters");
        String inviteCode = field(teamBody, "$.inviteCode");

        registerUser("bob@example.com", "bob", "password123");
        MockHttpSession memberSession = login("bob@example.com", "password123");

        mvc.perform(post("/api/teams/join")
                        .session(memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"%s\"}".formatted(inviteCode)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/teams/join")
                        .session(memberSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"%s\"}".formatted(inviteCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
    }

    @Test
    void joinTeam_withoutLogin_returns401() throws Exception {
        mvc.perform(post("/api/teams/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"ABCD1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}

