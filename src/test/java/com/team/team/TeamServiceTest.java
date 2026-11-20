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

import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

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
        lenient().when(currentUser.getId()).thenReturn(7L);
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
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

    private Team teamWithId(Long id, String name, String inviteCode) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", id);
        team.setName(name);
        team.setInviteCode(inviteCode);
        return team;
    }

    private TeamMember membership(Team team, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(currentUser);
        member.setRole(role);
        return member;
    }

    @Test
    void getMyTeams_returnsTeamsOfCurrentUserWithTheirRole() {
        Team owned = teamWithId(1L, "Alpha", "AAAA2222");
        Team joined = teamWithId(2L, "Beta", "BBBB3333");
        when(teamMemberRepository.findByUserId(7L)).thenReturn(List.of(
                membership(owned, TeamRole.OWNER),
                membership(joined, TeamRole.MEMBER)));

        List<MyTeamResponse> result = teamService.getMyTeams();

        assertEquals(2, result.size());
        assertEquals(new MyTeamResponse(1L, "Alpha", TeamRole.OWNER), result.get(0));
        assertEquals(new MyTeamResponse(2L, "Beta", TeamRole.MEMBER), result.get(1));
    }

    @Test
    void getMyTeams_whenUserHasNoTeams_returnsEmptyList() {
        when(teamMemberRepository.findByUserId(7L)).thenReturn(List.of());

        assertTrue(teamService.getMyTeams().isEmpty());
    }

    @Test
    void getTeam_asMember_returnsTeamWithInviteCode() {
        Team team = teamWithId(1L, "Alpha", "AAAA2222");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserId(1L, 7L)).thenReturn(true);

        TeamResponse response = teamService.getTeam(1L);

        assertEquals("Alpha", response.name());
        assertEquals("AAAA2222", response.inviteCode());
    }

    @Test
    void getTeam_whenNotMember_throwsForbidden() {
        Team team = teamWithId(1L, "Alpha", "AAAA2222");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserId(1L, 7L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> teamService.getTeam(1L));
    }

    @Test
    void getTeam_whenTeamDoesNotExist_throwsNotFound() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.getTeam(99L));
    }
}
