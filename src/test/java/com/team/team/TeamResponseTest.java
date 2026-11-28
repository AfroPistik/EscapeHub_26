
package com.team.team;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamResponseTest {

    @Test
    void constructor_preservesCreatedAtAndDefaultsNullMembersToEmptyList() {
        Instant createdAt = Instant.parse("2026-11-27T10:00:00Z");

        TeamResponse response = new TeamResponse(1L, "EscapeTeam", "INV12345", createdAt, null);

        assertEquals(1L, response.id());
        assertEquals("EscapeTeam", response.name());
        assertEquals("INV12345", response.inviteCode());
        assertEquals(createdAt, response.createdAt());
        assertTrue(response.members().isEmpty());
    }

    @Test
    void legacyConstructor_setsCreatedAtToNullAndCopiesMembers() {
        TeamResponse response = new TeamResponse(2L, "BetaTeam", "XYZ98765", List.of());

        assertNull(response.createdAt());
        assertTrue(response.members().isEmpty());
    }
}
