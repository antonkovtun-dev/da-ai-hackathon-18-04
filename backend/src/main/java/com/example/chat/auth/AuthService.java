package com.example.chat.auth;

import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.auth.dto.UserResponse;
import com.example.chat.common.FieldException;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new FieldException("email", "Email already in use");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new FieldException("username", "Username already taken");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        return UserResponse.from(userRepository.save(user));
    }
}
