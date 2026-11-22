
package com.team;

import com.team.user.CurrentUserService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
  
public class TestCurrentUserConfig {
  
  
    @Bean
    @ConditionalOnMissingBean(CurrentUserService.class)
    public CurrentUserService testCurrentUserService() {
        return Mockito.mock(CurrentUserService.class);
    }
}
