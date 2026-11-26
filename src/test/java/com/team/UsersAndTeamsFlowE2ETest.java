package com.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-17: end-to-end test της Απαίτησης 1 (χρήστες και ομάδες).
 *
 * Τρέχει όλη τη ροή με πραγματικό Spring context, πραγματικό security (session) και πραγματική
 * Postgres, χωρίς mocks. Χρησιμοποιεί τα helpers του IntegrationTestBase (US-16).
 *
 * Τα βήματα "remove" καλύπτουν και το US-14 (DELETE /api/teams/{id}/members/{userId}).
 */
class UsersAndTeamsFlowE2ETest extends IntegrationTestBase {

    private static final String PASSWORD = "password123";

    // ======================================================================
    // 1. Η κύρια ροή του sprint
    // ======================================================================

    @Test
    @DisplayName("register → login → create team → join → members → remove → ο αφαιρεθείς χάνει την πρόσβαση")
    void fullFlow_removedMemberLosesAccess() throws Exception {
        // register (2 χρήστες)
        registerUser("alice@example.com", "alice", PASSWORD);
        registerUser("bob@example.com", "bob", PASSWORD);

        // login
        MockHttpSession alice = login("alice@example.com", PASSWORD);
        MockHttpSession bob = login("bob@example.com", PASSWORD);

        // /me επιστρέφει τον τρέχοντα χρήστη, χωρίς κωδικό
        String meBody = mvc.perform(get("/api/users/me").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long aliceId = ((Number) field(meBody, "$.id")).longValue();
        long bobId = currentUserId(bob);

        // create team: ο alice γίνεται OWNER
        String teamBody = createTeam(alice, "Escape Masters");
        long teamId = ((Number) field(teamBody, "$.id")).longValue();
        String inviteCode = field(teamBody, "$.inviteCode");

        mvc.perform(get("/api/teams/mine").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(teamId))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        // join του δεύτερου χρήστη: γίνεται MEMBER
        joinTeam(bob, inviteCode);

        mvc.perform(get("/api/teams/mine").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(teamId))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));

        // members: δύο μέλη με σωστούς ρόλους, χωρίς προσωπικά δεδομένα
        String membersBody = mvc.perform(get("/api/teams/" + teamId + "/members").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Map<String, String> roles = rolesByUsername(membersBody);
        assertEquals("OWNER", roles.get("alice"));
        assertEquals("MEMBER", roles.get("bob"));
        assertFalse(membersBody.contains("@"), "Η λίστα μελών δεν πρέπει να περιέχει email");

        // ο bob (μέλος) έχει πρόσβαση στην ομάδα πριν την αφαίρεση
        mvc.perform(get("/api/teams/" + teamId).session(bob))
                .andExpect(status().isOk());

        // remove: ο owner αφαιρεί τον bob
        mvc.perform(delete("/api/teams/" + teamId + "/members/" + bobId).session(alice))
                .andExpect(status().isNoContent());

        // ο αφαιρεθείς δεν έχει πια πρόσβαση, και τα σφάλματα έχουν την κοινή μορφή
        expectCommonError(mvc.perform(get("/api/teams/" + teamId).session(bob)),
                status().isForbidden(), "NOT_A_MEMBER");
        expectCommonError(mvc.perform(get("/api/teams/" + teamId + "/members").session(bob)),
                status().isForbidden(), "FORBIDDEN");
        mvc.perform(get("/api/teams/mine").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // ο owner παραμένει, μόνος του
        String afterBody = mvc.perform(get("/api/teams/" + teamId + "/members").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        assertEquals("OWNER", rolesByUsername(afterBody).get("alice"));
        assertEquals(aliceId, ((Number) field(afterBody, "$[0].userId")).longValue());
    }

    // ======================================================================
    // 2. Κάθε προστατευμένο endpoint χωρίς login δίνει 401
    // ======================================================================

    @ParameterizedTest(name = "{0} {1} χωρίς login → 401")
    @CsvSource({
            "POST,   /api/auth/logout",
            "GET,    /api/users/me",
            "POST,   /api/teams",
            "GET,    /api/teams/mine",
            "GET,    /api/teams/1",
            "POST,   /api/teams/join",
            "GET,    /api/teams/1/members",
            "DELETE, /api/teams/1/members/me",
            "DELETE, /api/teams/1/members/2"
    })
    void protectedEndpoint_withoutLogin_returns401InCommonFormat(String method, String path) throws Exception {
        expectCommonError(mvc.perform(request(HttpMethod.valueOf(method), path)),
                status().isUnauthorized(), "UNAUTHORIZED");
    }

    @Test
    @DisplayName("Τα δημόσια endpoints (register, login) δεν δίνουν 401")
    void publicEndpoints_areReachableWithoutLogin() throws Exception {
        registerUser("alice@example.com", "alice", PASSWORD); // 201 χωρίς login
        login("alice@example.com", PASSWORD);                  // 200 χωρίς login
    }

    // ======================================================================
    // 3. Τα σφάλματα της ροής έχουν την κοινή μορφή ErrorResponse
    // ======================================================================

    @Test
    @DisplayName("Σφάλματα εγγραφής, login, δημιουργίας και ένταξης: κοινή μορφή ErrorResponse")
    void errorScenarios_useCommonErrorFormat() throws Exception {
        registerUser("alice@example.com", "alice", PASSWORD);
        registerUser("bob@example.com", "bob", PASSWORD);
        registerUser("charlie@example.com", "charlie", PASSWORD);
        MockHttpSession alice = login("alice@example.com", PASSWORD);
        MockHttpSession bob = login("bob@example.com", PASSWORD);
        MockHttpSession charlie = login("charlie@example.com", PASSWORD);

        // εγγραφή: διπλό email (με άλλα κεφαλαία) και διπλό username
        expectCommonError(postJson("/api/auth/register", null,
                        registerJson("ALICE@example.com", "alice2", PASSWORD)),
                status().isConflict(), "EMAIL_TAKEN");
        expectCommonError(postJson("/api/auth/register", null,
                        registerJson("alice2@example.com", "alice", PASSWORD)),
                status().isConflict(), "USERNAME_TAKEN");

        // εγγραφή: άκυρα δεδομένα → 400 με fieldErrors
        expectCommonError(postJson("/api/auth/register", null,
                        registerJson("not-an-email", "dave", PASSWORD)),
                status().isBadRequest(), "VALIDATION_ERROR")
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        // login: λάθος κωδικός και ανύπαρκτος χρήστης δίνουν το ίδιο 401
        expectCommonError(postJson("/api/auth/login", null, loginJson("alice@example.com", "wrong-password")),
                status().isUnauthorized(), "INVALID_CREDENTIALS");
        expectCommonError(postJson("/api/auth/login", null, loginJson("nobody@example.com", PASSWORD)),
                status().isUnauthorized(), "INVALID_CREDENTIALS");

        // δημιουργία ομάδας: άκυρο όνομα → 400 με fieldErrors
        expectCommonError(postJson("/api/teams", alice, "{\"name\":\"ab\"}"),
                status().isBadRequest(), "VALIDATION_ERROR")
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        String teamBody = createTeam(alice, "Escape Masters");
        long teamId = ((Number) field(teamBody, "$.id")).longValue();
        String inviteCode = field(teamBody, "$.inviteCode");

        // ένταξη: άκυρος κωδικός → 404, ήδη μέλος → 409
        expectCommonError(postJson("/api/teams/join", bob, "{\"inviteCode\":\"ZZZZZZZZ\"}"),
                status().isNotFound(), "INVALID_INVITE_CODE");
        joinTeam(bob, inviteCode);
        expectCommonError(postJson("/api/teams/join", bob, "{\"inviteCode\":\"%s\"}".formatted(inviteCode)),
                status().isConflict(), "ALREADY_MEMBER");

        // ο κωδικός δεν διακρίνει πεζά/κεφαλαία
        joinTeam(charlie, inviteCode.toLowerCase());

        // ο owner δεν μπορεί να αποχωρήσει
        expectCommonError(mvc.perform(delete("/api/teams/" + teamId + "/members/me").session(alice)),
                status().isConflict(), "OWNER_CANNOT_LEAVE");

        // ανύπαρκτη ομάδα → 404
        expectCommonError(mvc.perform(get("/api/teams/9999").session(alice)),
                status().isNotFound(), "TEAM_NOT_FOUND");
        expectCommonError(mvc.perform(get("/api/teams/9999/members").session(alice)),
                status().isNotFound(), "TEAM_NOT_FOUND");
    }

    @Test
    @DisplayName("Μη μέλος: 403 στα endpoints της ομάδας, και η αποχώρηση ανοίγει ξανά την ένταξη")
    void nonMemberAndRejoin() throws Exception {
        registerUser("alice@example.com", "alice", PASSWORD);
        registerUser("bob@example.com", "bob", PASSWORD);
        MockHttpSession alice = login("alice@example.com", PASSWORD);
        MockHttpSession bob = login("bob@example.com", PASSWORD);

        String teamBody = createTeam(alice, "Escape Masters");
        long teamId = ((Number) field(teamBody, "$.id")).longValue();
        String inviteCode = field(teamBody, "$.inviteCode");

        // ο bob δεν είναι ακόμα μέλος: 403 και ο κωδικός δεν διαρρέει
        String forbiddenBody = expectCommonError(mvc.perform(get("/api/teams/" + teamId).session(bob)),
                status().isForbidden(), "NOT_A_MEMBER")
                .andReturn().getResponse().getContentAsString();
        assertFalse(forbiddenBody.contains(inviteCode), "Το inviteCode δεν πρέπει να εμφανίζεται σε μη μέλος");

        // join → leave → ξανά join
        joinTeam(bob, inviteCode);
        mvc.perform(delete("/api/teams/" + teamId + "/members/me").session(bob))
                .andExpect(status().isNoContent());
        expectCommonError(mvc.perform(get("/api/teams/" + teamId + "/members").session(bob)),
                status().isForbidden(), "FORBIDDEN");
        joinTeam(bob, inviteCode);
        mvc.perform(get("/api/teams/" + teamId + "/members").session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ======================================================================
    // 4. Κανόνες αφαίρεσης μέλους (US-14)
    // ======================================================================

    @Test
    @DisplayName("Αφαίρεση μέλους (US-14): 403 για MEMBER, 409 για τον owner στον εαυτό του, 404 για μη μέλος")
    void removeMember_rules() throws Exception {
        registerUser("alice@example.com", "alice", PASSWORD);
        registerUser("bob@example.com", "bob", PASSWORD);
        registerUser("charlie@example.com", "charlie", PASSWORD);
        MockHttpSession alice = login("alice@example.com", PASSWORD);
        MockHttpSession bob = login("bob@example.com", PASSWORD);
        MockHttpSession charlie = login("charlie@example.com", PASSWORD);
        long aliceId = currentUserId(alice);
        long charlieId = currentUserId(charlie);

        String teamBody = createTeam(alice, "Escape Masters");
        long teamId = ((Number) field(teamBody, "$.id")).longValue();
        joinTeam(bob, field(teamBody, "$.inviteCode"));

        // MEMBER δεν μπορεί να αφαιρέσει (ακόμη και με άμεσο αίτημα στο backend)
        expectCommonError(mvc.perform(delete("/api/teams/" + teamId + "/members/" + aliceId).session(bob)),
                status().isForbidden());

        // ο owner δεν μπορεί να αφαιρέσει τον εαυτό του
        expectCommonError(mvc.perform(delete("/api/teams/" + teamId + "/members/" + aliceId).session(alice)),
                status().isConflict(), "OWNER_CANNOT_REMOVE_SELF");

        // αφαίρεση χρήστη που δεν είναι μέλος
        expectCommonError(mvc.perform(delete("/api/teams/" + teamId + "/members/" + charlieId).session(alice)),
                status().isNotFound(), "NOT_A_MEMBER");
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    /** Ελέγχει status και ότι το σώμα έχει την κοινή μορφή ErrorResponse (code, message, timestamp). */
    private ResultActions expectCommonError(ResultActions result, ResultMatcher expectedStatus) throws Exception {
        return result
                .andExpect(expectedStatus)
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private ResultActions expectCommonError(ResultActions result, ResultMatcher expectedStatus, String expectedCode)
            throws Exception {
        return expectCommonError(result, expectedStatus)
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private ResultActions postJson(String path, MockHttpSession session, String json) throws Exception {
        var builder = post(path).contentType(MediaType.APPLICATION_JSON).content(json);
        if (session != null) {
            builder.session(session);
        }
        return mvc.perform(builder);
    }

    private static String registerJson(String email, String username, String password) {
        return "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}".formatted(email, username, password);
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    private long currentUserId(MockHttpSession session) throws Exception {
        String body = mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) field(body, "$.id")).longValue();
    }

    private Map<String, String> rolesByUsername(String membersJson) {
        List<Map<String, Object>> members = field(membersJson, "$");
        Map<String, String> roles = new HashMap<>();
        for (Map<String, Object> member : members) {
            roles.put((String) member.get("username"), (String) member.get("role"));
        }
        return roles;
    }
}
