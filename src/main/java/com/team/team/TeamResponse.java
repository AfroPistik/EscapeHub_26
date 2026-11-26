
package com.team.team;

import java.time.Instant;
import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String inviteCode,
        Instant createdAt,
        List<TeamMemberResponse> members
) {

    public TeamResponse {
        members = members == null ? List.of() : List.copyOf(members);
    }

    public TeamResponse(Long id, String name, String inviteCode, List<TeamMemberResponse> members) {
        this(id, name, inviteCode, null, members);
    }

    public static TeamResponse from(Team team) {
        return from(team, List.of());
    }

    public static TeamResponse from(Team team, List<TeamMemberResponse> members) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                team.getCreatedAt(),
                members
        );
    }
}
