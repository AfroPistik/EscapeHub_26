package com.team.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.common.error.ConflictException;
import com.team.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    void register_savesUserWithHashedPasswordAndLowercaseEmail() {
        when(passwordEncoder.encode("12345678")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(new RegisterRequest("Test@Example.com", "testuser", "12345678"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getPasswordHash()).isNotEqualTo("12345678");
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getUsername()).isEqualTo("testuser");
    }

    @Test
    void register_duplicateEmail_throwsEmailTaken() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(new RegisterRequest("test@example.com", "testuser", "12345678")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo(ErrorCode.EMAIL_TAKEN);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsUsernameTaken() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(new RegisterRequest("test@example.com", "testuser", "12345678")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo(ErrorCode.USERNAME_TAKEN);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_concurrentDuplicateEmail_throwsEmailTakenNotServerError() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false, true);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() ->
                userService.register(new RegisterRequest("test@example.com", "testuser", "12345678")))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo(ErrorCode.EMAIL_TAKEN);
    }
}
