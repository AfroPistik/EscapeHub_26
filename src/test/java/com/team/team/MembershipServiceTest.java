
package com.team.team;

import com.team.common.error.ConflictException;
import com.team.common.error.NotFoundException;
import com.team.user.CurrentUserService;
import com.team.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(teamRepository, teamMemberRepository, currentUserService);
    }

    @Test
    void joinValidInviteCodeAddsMemberAndReturnsTeamResponse() {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", 10L);
        team.setName("Alpha");
        team.setInviteCode("ABCD2345");

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("player1");

        TeamMember savedMember = new TeamMember();
        savedMember.setTeam(team);
        savedMember.setUser(user);
        savedMember.setRole(TeamRole.MEMBER);

        when(teamRepository.findByInviteCodeIgnoreCase("abcd2345")).thenReturn(Optional.of(team));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);
        when(teamMemberRepository.findByTeamId(10L)).thenReturn(List.of(savedMember));

        TeamResponse response = membershipService.join("  abcd2345  ");

        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).saveAndFlush(captor.capture());
        assertEquals(TeamRole.MEMBER, captor.getValue().getRole());
        assertEquals(10L, response.id());
        assertEquals("Alpha", response.name());
        assertEquals("ABCD2345", response.inviteCode());
        assertEquals(1, response.members().size());
        assertEquals("player1", response.members().get(0).username());
    }

    @Test
    void joinInvalidInviteCodeThrowsNotFound() {
        when(teamRepository.findByInviteCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> membershipService.join("INVALID"));
    }

    
    @Test
    void joinWhenAlreadyMemberThrowsConflict() {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", 10L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(teamRepository.findByInviteCodeIgnoreCase("ABCD2345")).thenReturn(Optional.of(team));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> membershipService.join("ABCD2345"));
    }

    @Test
    void joinConcurrentDuplicateThrowsConflict() {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", 10L);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(teamRepository.findByInviteCodeIgnoreCase("ABCD2345")).thenReturn(Optional.of(team));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);
        when(teamMemberRepository.saveAndFlush(any(TeamMember.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> membershipService.join("ABCD2345"));
    }
}

