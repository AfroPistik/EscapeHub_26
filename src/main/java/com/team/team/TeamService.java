package com.team.team;

import com.team.user.CurrentUserService;
import com.team.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.team.common.error.ErrorCode;
import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;

@Service
public class TeamService {

    private static final int MAX_RETRIES = 10;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;
    private final InviteCodeGenerator inviteCodeGenerator;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            CurrentUserService currentUserService,
            InviteCodeGenerator inviteCodeGenerator
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
        this.inviteCodeGenerator = inviteCodeGenerator;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        String trimmedName = request.name().trim();

        Team savedTeam = saveTeamWithUniqueInviteCode(trimmedName);

        TeamMember ownerMembership = new TeamMember();
        ownerMembership.setTeam(savedTeam);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(TeamRole.OWNER);
        teamMemberRepository.save(ownerMembership);

        return TeamResponse.from(savedTeam);
    }

    @Transactional(readOnly = true)
    public List<MyTeamResponse> getMyTeams() {
        Long userId = currentUserService.getCurrentUserId();
        return teamMemberRepository.findByUserId(userId).stream()
                .map(MyTeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "Team not found"));

        Long userId = currentUserService.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ForbiddenException(ErrorCode.NOT_A_MEMBER, "You are not a member of this team");
        }

        return TeamResponse.from(team);
    }

    private Team saveTeamWithUniqueInviteCode(String name) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String candidateCode = generateNonCollidingCode();
            Team team = new Team();
            team.setName(name);
            team.setInviteCode(candidateCode);
            try {
                return teamRepository.saveAndFlush(team);
            } catch (DataIntegrityViolationException ex) {
                if (attempt == MAX_RETRIES - 1) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Failed to generate a unique invite code");
    }

    private String generateNonCollidingCode() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String code = inviteCodeGenerator.generate();
            if (!teamRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique invite code");
    }
}
