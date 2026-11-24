package com.team.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityCurrentUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SecurityCurrentUserService service = new SecurityCurrentUserService(userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsLoggedInUserAndItsId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setEmail("maria@example.com");
        when(userRepository.findByEmailIgnoreCase("maria@example.com")).thenReturn(Optional.of(user));
        loginAs("maria@example.com");

        assertSame(user, service.getCurrentUser());
        assertEquals(Long.valueOf(7L), service.getCurrentUserId());
    }

    @Test
    void throwsWhenNobodyIsLoggedIn() {
        assertThrows(IllegalStateException.class, service::getCurrentUser);
    }

    @Test
    void throwsForAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThrows(IllegalStateException.class, service::getCurrentUser);
    }

    @Test
    void throwsWhenLoggedInUserNoLongerExists() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        loginAs("ghost@example.com");

        assertThrows(IllegalStateException.class, service::getCurrentUser);
    }

    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
    }
}