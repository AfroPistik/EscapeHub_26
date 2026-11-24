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

    // Για τον κώδικα των memberships (join/members/leave): χωρίς createdAt, με λίστα μελών.
    public TeamResponse(Long id, String name, String inviteCode, List<TeamMemberResponse> members) {
        this(id, name, inviteCode, null, members);
    }

    // Για τη δημιουργία ομάδας και τα στοιχεία ομάδας (US-09/US-10): με createdAt, χωρίς μέλη.
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                team.getCreatedAt(),
                List.of()
        );
    }
}