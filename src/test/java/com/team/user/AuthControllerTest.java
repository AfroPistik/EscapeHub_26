package com.team.user;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.config.SecurityConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(classes = {SecurityConfig.class, AuthControllerTest.TestConfig.class})
class AuthControllerTest {

    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void loginWithCorrectCredentialsGives200AndStartsSession() throws Exception {
        MockHttpSession session = (MockHttpSession) login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("maria"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn().getRequest().getSession(false);
        assertNotNull(session);
    }

    @Test
    void loginEmailIsCaseInsensitive() throws Exception {
        login("MARIA@Example.COM", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void wrongPasswordGives401() throws Exception {
        login(EMAIL, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unknownUserGives401WithSameMessage() throws Exception {
        login("nobody@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meWithoutSessionGives401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithSessionGivesCurrentUser() throws Exception {
        MockHttpSession session = loggedInSession();
        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.username").value("maria"));
    }

    @Test
    void logoutGives204AndThenMeGives401() throws Exception {
        MockHttpSession session = loggedInSession();

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());

        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private MockHttpSession loggedInSession() throws Exception {
        return (MockHttpSession) login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestConfig {

        // Ψεύτικο repository: ένας χρήστης, με κωδικό hashed από τον πραγματικό PasswordEncoder
        @Bean
        UserRepository userRepository(PasswordEncoder passwordEncoder) {
            User user = new User();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.setEmail(EMAIL);
            user.setUsername("maria");
            user.setPasswordHash(passwordEncoder.encode(PASSWORD));

            UserRepository repository = mock(UserRepository.class);
            when(repository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation ->
                    EMAIL.equalsIgnoreCase(invocation.getArgument(0))
                            ? Optional.of(user)
                            : Optional.empty());
            return repository;
        }

        @Bean
        DatabaseUserDetailsService userDetailsService(UserRepository userRepository) {
            return new DatabaseUserDetailsService(userRepository);
        }

        // Ψεύτικο UserService: τα tests του login δεν χρησιμοποιούν το register
        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        AuthController authController(AuthenticationManager authenticationManager,
                                      UserRepository userRepository,
                                      UserService userService) {
            return new AuthController(authenticationManager, userRepository, userService);
        }
    }
}