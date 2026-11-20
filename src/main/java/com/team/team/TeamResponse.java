
package com.team.team;

import java.util.List;

public record TeamResponse(Long id, String name, String inviteCode, List<TeamMemberResponse> members) {

    public TeamResponse {
        members = members == null ? List.of() : List.copyOf(members);
    }
}

