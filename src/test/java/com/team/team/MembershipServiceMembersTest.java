
package com.team.team;

import com.team.common.error.ErrorCode;
import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;
import com.team.user.CurrentUserService;
import com.team.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceMembersTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void getMembers_returnsList_whenUserIsMember() {
        Long teamId = 10L;
        User currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(1L);
        when(currentUser.getUsername()).thenReturn("player1");

        TeamMember member = mock(TeamMember.class);
        Instant joinedAt = Instant.parse("2026-11-20T10:00:00Z");
        when(member.getUser()).thenReturn(currentUser);
        when(member.getRole()).thenReturn(TeamRole.OWNER);
        when(member.getJoinedAt()).thenReturn(joinedAt);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, 1L)).thenReturn(true);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));

        List<TeamMemberResponse> result = membershipService.getMembers(teamId);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
        assertEquals("player1", result.get(0).username());
        assertEquals(TeamRole.OWNER, result.get(0).role());
        assertEquals(joinedAt, result.get(0).joinedAt());
    }

    @Test
    void getMembers_throwsNotFound_whenTeamDoesNotExist() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> membershipService.getMembers(99L));
    }

    @Test
    void getMembers_throwsForbidden_whenUserIsNotMember() {
        Long teamId = 10L;
        User outsider = mock(User.class);
        when(outsider.getId()).thenReturn(2L);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, 2L)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> membershipService.getMembers(teamId));
        assertEquals(ErrorCode.NOT_A_MEMBER, ex.getCode());
    }

    @Test
    void leave_throwsNotAMember_whenUserIsNotMember() {
        Long teamId = 10L;
        User outsider = mock(User.class);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> membershipService.leave(teamId));
        assertEquals(ErrorCode.NOT_A_MEMBER, ex.getCode());
    }

    @Test
    void removeMember_throwsNotAMember_whenRequesterIsNotMember() {
        Long teamId = 10L;
        User outsider = mock(User.class);
        when(outsider.getId()).thenReturn(2L);

        when(teamRepository.existsById(teamId)).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, 2L)).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> membershipService.removeMember(teamId, 3L));
        assertEquals(ErrorCode.NOT_A_MEMBER, ex.getCode());
    }
}

