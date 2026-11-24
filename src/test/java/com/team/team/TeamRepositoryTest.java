package com.team.team;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TeamRepositoryTest {

    @Autowired
    TeamRepository teams;

    @Test
    void duplicateInviteCodeIsRejected() {
        teams.saveAndFlush(newTeam("Team A", "ABCD2345"));
        assertThrows(DataIntegrityViolationException.class,
                () -> teams.saveAndFlush(newTeam("Team B", "ABCD2345")));
    }

    @Test
    void findByInviteCodeAndExistsByInviteCode() {
        teams.saveAndFlush(newTeam("Team A", "ABCD2345"));
        assertTrue(teams.findByInviteCode("ABCD2345").isPresent());
        assertTrue(teams.existsByInviteCode("ABCD2345"));
        assertFalse(teams.existsByInviteCode("ZZZZ9999"));
    }

    @Test
    void createdAtIsGeneratedByDatabaseOnInsert() {
        Team saved = teams.saveAndFlush(newTeam("Team A", "ABCD2345"));
        assertNotNull(saved.getCreatedAt());
    }

    private Team newTeam(String name, String inviteCode) {
        Team t = new Team();
        t.setName(name);
        t.setInviteCode(inviteCode);
        return t;
    }
}