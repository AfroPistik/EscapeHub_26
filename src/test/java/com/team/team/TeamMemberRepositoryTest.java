package com.team.team;

import com.team.user.User;
import com.team.user.UserRepository;
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
class TeamMemberRepositoryTest {

    @Autowired TeamMemberRepository members;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;

    @Test
    void duplicateTeamUserPairIsRejected() {
        Team team = saveTeam("ABCD2345");
        User alice = saveUser("alice");
        members.saveAndFlush(member(team, alice, TeamRole.OWNER));
        assertThrows(DataIntegrityViolationException.class,
                () -> members.saveAndFlush(member(team, alice, TeamRole.MEMBER)));
    }

    @Test
    void existsAndFindQueries() {
        Team team = saveTeam("ABCD2345");
        User alice = saveUser("alice");
        User bob = saveUser("bob");
        members.saveAndFlush(member(team, alice, TeamRole.OWNER));
        members.saveAndFlush(member(team, bob, TeamRole.MEMBER));

        assertTrue(members.existsByTeamIdAndUserId(team.getId(), alice.getId()));
        assertFalse(members.existsByTeamIdAndUserId(team.getId(), 999999L));
        assertEquals(2, members.findByTeamId(team.getId()).size());
        assertEquals(TeamRole.MEMBER,
                members.findByTeamIdAndUserId(team.getId(), bob.getId()).orElseThrow().getRole());
        assertEquals(1, members.findByUserId(alice.getId()).size());
    }

    // Αν πρόσθεσες το partial unique index uq_team_members_one_owner στο V3:
    @Test
    void secondOwnerInSameTeamIsRejected() {
        Team team = saveTeam("ABCD2345");
        members.saveAndFlush(member(team, saveUser("alice"), TeamRole.OWNER));
        User bob = saveUser("bob");
        assertThrows(DataIntegrityViolationException.class,
                () -> members.saveAndFlush(member(team, bob, TeamRole.OWNER)));
    }

    @Test
    void joinedAtIsGeneratedByDatabaseOnInsert() {
        Team team = saveTeam("ABCD2345");
        TeamMember saved = members.saveAndFlush(member(team, saveUser("alice"), TeamRole.OWNER));
        assertNotNull(saved.getJoinedAt());
    }

    private Team saveTeam(String code) {
        Team t = new Team();
        t.setName("Team " + code);
        t.setInviteCode(code);
        return teams.saveAndFlush(t);
    }

    private User saveUser(String username) {
        User u = new User();
        u.setEmail(username + "@x.com");
        u.setUsername(username);
        u.setPasswordHash("hash");
        return users.saveAndFlush(u);
    }

    private TeamMember member(Team team, User user, TeamRole role) {
        TeamMember m = new TeamMember();
        m.setTeam(team);
        m.setUser(user);
        m.setRole(role);
        return m;
    }
}