package com.team.team;

import com.team.common.error.ErrorCode;
import com.team.common.error.ForbiddenException;
import com.team.common.error.NotFoundException;
import com.team.user.CurrentUserService;
import com.team.user.User;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class TeamService {

    private static final int MAX_RETRIES = 10;
    private static final String INVITE_CODE_CONSTRAINT = "uq_teams_invite_code";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CurrentUserService currentUserService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final TransactionTemplate transactionTemplate;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            CurrentUserService currentUserService,
            InviteCodeGenerator inviteCodeGenerator,
            TransactionTemplate transactionTemplate
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.currentUserService = currentUserService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.transactionTemplate = transactionTemplate;
    }

    public TeamResponse createTeam(CreateTeamRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        String name = request.name().trim();

        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> saveTeamWithOwner(name, currentUser));
            } catch (DataIntegrityViolationException ex) {
                if (!isInviteCodeCollision(ex) || attempt >= MAX_RETRIES) {
                    throw ex;
                }
            }
        }
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

    private TeamResponse saveTeamWithOwner(String name, User owner) {
        Team team = new Team();
        team.setName(name);
        team.setInviteCode(inviteCodeGenerator.generate());
        Team savedTeam = teamRepository.saveAndFlush(team);

        TeamMember ownerMembership = new TeamMember();
        ownerMembership.setTeam(savedTeam);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(TeamRole.OWNER);
        teamMemberRepository.save(ownerMembership);

        return TeamResponse.from(savedTeam);
    }

    private boolean isInviteCodeCollision(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                return constraint != null && constraint.equalsIgnoreCase(INVITE_CODE_CONSTRAINT);
            }
        }
        return false;
    }
}