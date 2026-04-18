package com.example.chat.unit;

import com.example.chat.auth.AuthService;
import com.example.chat.common.FieldException;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    void register_rejects_duplicate_email() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        FieldException ex = assertThrows(FieldException.class,
                () -> authService.register(new RegisterRequest("dup@example.com", "user1", "password123")));

        assertThat(ex.getField()).isEqualTo("email");
    }

    @Test
    void register_rejects_duplicate_username() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        FieldException ex = assertThrows(FieldException.class,
                () -> authService.register(new RegisterRequest("new@example.com", "taken", "password123")));

        assertThat(ex.getField()).isEqualTo("username");
    }

    @Test
    void register_stores_password_as_hash_not_plaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$fakehash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequest("test@example.com", "testuser", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$fakehash");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("password123");
    }

    @Test
    void register_persists_email_and_username() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequest("alice@example.com", "alice", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }
}
