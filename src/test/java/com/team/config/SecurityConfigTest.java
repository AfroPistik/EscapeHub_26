package com.team.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(classes = {SecurityConfig.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void protectedEndpointWithoutLoginGives401InCommonFormat() throws Exception {
        mvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void protectedEndpointWithLoggedInUserIsAccessible() throws Exception {
        mvc.perform(get("/api/protected").with(user("test@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        mvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpointIsPublic() throws Exception {
        mvc.perform(post("/api/auth/register"))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestConfig {
        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/protected")
        String protectedEndpoint() {
            return "ok";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "ok";
        }

        @PostMapping("/api/auth/register")
        String register() {
            return "ok";
        }
    }
}