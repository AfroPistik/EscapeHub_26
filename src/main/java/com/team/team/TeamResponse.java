package com.team.team;

import java.time.Instant;

public record TeamResponse(
        Long id,
        String name,
        String inviteCode,
        Instant createdAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                team.getCreatedAt()
        );
    }
}