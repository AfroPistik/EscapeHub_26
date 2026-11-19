package com.team.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository users;

    @Test
    void duplicateUsernameIsRejected() {
        users.saveAndFlush(newUser("a@x.com", "alice"));
      assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(newUser("b@x.com", "alice")));
    }

    @Test
    void duplicateEmailIsRejected() {
        users.saveAndFlush(newUser("a@x.com", "alice"));
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(newUser("A@X.com", "bob")));
    }

    @Test
    void findByEmailIgnoreCaseFindsUser() {
        users.saveAndFlush(newUser("a@x.com", "alice"));
        assertTrue(users.findByEmailIgnoreCase("A@X.COM").isPresent());
    }

    private User newUser(String email, String username) {
        User u = new User();
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash("hash");
        return u;
    }
    @Test
    void existsByEmailIgnoreCaseAndUsername() {
        users.saveAndFlush(newUser("a@x.com", "alice"));
        assertTrue(users.existsByEmailIgnoreCase("A@X.COM"));
        assertTrue(users.existsByUsername("alice"));
        assertFalse(users.existsByUsername("nobody"));
    }
}