
package com.team.team;

import com.team.common.error.ConflictException;
import com.team.common.error.ErrorCode;
import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;
import com.team.user.CurrentUserService;
import com.team.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MembershipService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;

    public MembershipService(TeamRepository teamRepository,
                             TeamMemberRepository teamMemberRepository,
                             CurrentUserService currentUserService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TeamResponse join(String rawInviteCode) {
        String inviteCode = rawInviteCode == null ? "" : rawInviteCode.trim();

        Team team = teamRepository.findByInviteCodeIgnoreCase(inviteCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INVITE_CODE, "Invalid invite code"));

        User user = currentUserService.getCurrentUser();

        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), user.getId())) {
            throw new ConflictException(ErrorCode.ALREADY_MEMBER, "User is already a member of this team");
        }

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.MEMBER);

        try {
            teamMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(ErrorCode.ALREADY_MEMBER, "User is already a member of this team");
        }

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId())
                .stream()
                .map(TeamMemberResponse::from)
                .toList();

        return new TeamResponse(team.getId(), team.getName(), team.getInviteCode(), members);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "Team not found");
        }

        User user = currentUserService.getCurrentUser();

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN, "User is not a member of this team");
        }

        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(TeamMemberResponse::from)
                .toList();
    }
}
