package com.team.team;

import com.team.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamQueriesIntegrationTest extends IntegrationTestBase {

    @Test
    void getMyTeams_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/teams/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTeam_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/teams/1"))
                .andExpect(status().isUnauthorized());
    }
}
