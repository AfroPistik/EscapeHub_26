
package com.team.team;

import java.time.Instant;

public record TeamMemberResponse(Long userId, String username, TeamRole role, Instant joinedAt) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
