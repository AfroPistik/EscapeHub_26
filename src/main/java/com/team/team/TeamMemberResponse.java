
package com.team.team;

public record TeamMemberResponse(Long userId, String username, TeamRole role) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole()
        );
    }
}
