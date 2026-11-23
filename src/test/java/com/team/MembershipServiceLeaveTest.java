
package com.team.team;

import com.team.common.error.ConflictException;
import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;
import com.team.user.CurrentUserService;
import com.team.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceLeaveTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void leave_deletesMember_whenUserIsMember() {
        Long teamId = 10L;
        User currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(2L);

        TeamMember member = mock(TeamMember.class);
        when(member.getUser()).thenReturn(currentUser);
        when(member.getRole()).thenReturn(TeamRole.MEMBER);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));

        membershipService.leave(teamId);

        verify(teamMemberRepository).delete(member);
    }

    @Test
    void leave_throwsConflict_whenUserIsOwner() {
        Long teamId = 10L;
        User ownerUser = mock(User.class);
        when(ownerUser.getId()).thenReturn(1L);

        TeamMember ownerMember = mock(TeamMember.class);
        when(ownerMember.getUser()).thenReturn(ownerUser);
        when(ownerMember.getRole()).thenReturn(TeamRole.OWNER);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(ownerUser);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(ownerMember));

        assertThrows(ConflictException.class, () -> membershipService.leave(teamId));
        verify(teamMemberRepository, never()).delete(ownerMember);
    }

    @Test
    void leave_throwsNotFound_whenTeamDoesNotExist() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> membershipService.leave(99L));
    }

    @Test
    void leave_throwsForbidden_whenUserIsNotMember() {
        Long teamId = 10L;
        User outsider = mock(User.class);
        when(outsider.getId()).thenReturn(3L);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of());

        assertThrows(ForbiddenException.class, () -> membershipService.leave(teamId));
    }
}
