package com.team.team;

public record MyTeamResponse(
        Long id,
        String name,
        TeamRole role
) {
    public static MyTeamResponse from(TeamMember membership) {
        Team team = membership.getTeam();
        return new MyTeamResponse(team.getId(), team.getName(), membership.getRole());
    }
}