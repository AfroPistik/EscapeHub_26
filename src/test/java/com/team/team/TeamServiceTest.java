package com.team.team;

import com.team.user.CurrentUserService;
import com.team.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private InviteCodeGenerator inviteCodeGenerator;

    private TeamService teamService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                teamRepository,
                teamMemberRepository,
                currentUserService,
                inviteCodeGenerator
        );
        currentUser = mock(User.class);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createTeam_createsTeamAndAssignsCreatorAsOwner() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
        when(teamRepository.existsByInviteCode("ABCD2345")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService.createTeam(new CreateTeamRequest("  Escape Masters  "));

        assertNotNull(response);
        assertEquals("Escape Masters", response.name());
        assertEquals("ABCD2345", response.inviteCode());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertEquals(TeamRole.OWNER, memberCaptor.getValue().getRole());
        assertEquals(currentUser, memberCaptor.getValue().getUser());
        assertEquals("Escape Masters", memberCaptor.getValue().getTeam().getName());
    }

    @Test
    void createTeam_retriesWhenInviteCodeCollides() {
        when(inviteCodeGenerator.generate()).thenReturn("DUP12345", "NEW98765");
        when(teamRepository.existsByInviteCode("DUP12345")).thenReturn(true);
        when(teamRepository.existsByInviteCode("NEW98765")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService.createTeam(new CreateTeamRequest("Puzzle Squad"));

        assertEquals("NEW98765", response.inviteCode());
        verify(inviteCodeGenerator, times(2)).generate();
    }

    @Test
    void createTeam_whenSavingOwnerFails_throwsAndDoesNotReturnTeamResponse() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
        when(teamRepository.existsByInviteCode("ABCD2345")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("DB error"))
                .when(teamMemberRepository).save(any(TeamMember.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> teamService.createTeam(new CreateTeamRequest("Rollback Team")));
    }

    @Test
    void createTeam_whenAllRetriesFail_throwsDataIntegrityViolationException() {
        when(inviteCodeGenerator.generate()).thenReturn("SAME1234");
        when(teamRepository.existsByInviteCode("SAME1234")).thenReturn(false);
        when(teamRepository.saveAndFlush(any(Team.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint"));

        assertThrows(DataIntegrityViolationException.class,
                () -> teamService.createTeam(new CreateTeamRequest("Retry Limit Team")));

        verify(teamMemberRepository, never()).save(any());
    }
}
