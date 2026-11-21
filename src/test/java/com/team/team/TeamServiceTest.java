package com.team.team;

import com.team.user.CurrentUserService;
import com.team.user.User;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private TransactionTemplate transactionTemplate;

    private TeamService teamService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                teamRepository,
                teamMemberRepository,
                currentUserService,
                inviteCodeGenerator,
                transactionTemplate
        );
        currentUser = mock(User.class);
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // Το TransactionTemplate εκτελεί απλώς το callback (χωρίς πραγματική transaction)
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private DataIntegrityViolationException violationOf(String constraintName) {
        ConstraintViolationException cause = mock(ConstraintViolationException.class);
        when(cause.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("constraint violation", cause);
    }

    @Test
    void createTeam_createsTeamAndAssignsCreatorAsOwner() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
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
        DataIntegrityViolationException collision = violationOf("uq_teams_invite_code");
        when(inviteCodeGenerator.generate()).thenReturn("DUP12345", "NEW98765");
        when(teamRepository.saveAndFlush(any(Team.class)))
                .thenThrow(collision)
                .thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService.createTeam(new CreateTeamRequest("Puzzle Squad"));

        assertEquals("NEW98765", response.inviteCode());
        verify(inviteCodeGenerator, times(2)).generate();
        verify(teamMemberRepository, times(1)).save(any(TeamMember.class));
    }

    @Test
    void createTeam_whenAllRetriesCollide_throwsDataIntegrityViolationException() {
        DataIntegrityViolationException collision = violationOf("uq_teams_invite_code");
        when(inviteCodeGenerator.generate()).thenReturn("SAME1234");
        when(teamRepository.saveAndFlush(any(Team.class))).thenThrow(collision);

        assertThrows(DataIntegrityViolationException.class,
                () -> teamService.createTeam(new CreateTeamRequest("Retry Limit Team")));

        verify(inviteCodeGenerator, times(10)).generate();
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void createTeam_whenOtherConstraintFails_throwsWithoutRetry() {
        DataIntegrityViolationException other = violationOf("some_other_constraint");
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
        when(teamRepository.saveAndFlush(any(Team.class))).thenThrow(other);

        assertThrows(DataIntegrityViolationException.class,
                () -> teamService.createTeam(new CreateTeamRequest("Other Constraint")));

        verify(inviteCodeGenerator, times(1)).generate();
    }

    @Test
    void createTeam_whenSavingOwnerFails_throwsWithoutRetry() {
        when(inviteCodeGenerator.generate()).thenReturn("ABCD2345");
        when(teamRepository.saveAndFlush(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("DB error"))
                .when(teamMemberRepository).save(any(TeamMember.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> teamService.createTeam(new CreateTeamRequest("Rollback Team")));

        verify(inviteCodeGenerator, times(1)).generate();
    }
}