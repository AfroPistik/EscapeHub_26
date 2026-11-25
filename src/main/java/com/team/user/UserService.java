package com.team.user;

import com.team.common.error.ConflictException;
import com.team.common.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        String email = request.email().trim();
        String username = request.username();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorCode.EMAIL_TAKEN, "Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException(ErrorCode.USERNAME_TAKEN, "Username is already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException(ErrorCode.EMAIL_TAKEN, "Email is already registered");
            }
            if (userRepository.existsByUsername(username)) {
                throw new ConflictException(ErrorCode.USERNAME_TAKEN, "Username is already taken");
            }
            throw e;
        }
    }
}
