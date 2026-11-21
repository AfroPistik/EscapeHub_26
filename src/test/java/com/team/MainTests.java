package com.team;

import com.team.user.CurrentUserService;
import com.team.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MainTests.TestCurrentUserConfig.class)
class MainTests {

    @TestConfiguration
    static class TestCurrentUserConfig {
        @Bean
        CurrentUserService currentUserService() {
            return new CurrentUserService() {
                @Override
                public User getCurrentUser() {
                    return null;
                }

                @Override
                public Long getCurrentUserId() {
                    return null;
                }
            };
        }
    }

    @Test
    void contextLoads() {
    }
}