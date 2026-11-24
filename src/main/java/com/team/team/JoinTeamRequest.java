
package com.team.team;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode
) {}