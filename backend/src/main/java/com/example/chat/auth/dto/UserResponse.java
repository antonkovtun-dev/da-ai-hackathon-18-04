package com.example.chat.auth.dto;

import com.example.chat.users.User;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String email, String username, OffsetDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getUsername(), user.getCreatedAt());
    }
}
