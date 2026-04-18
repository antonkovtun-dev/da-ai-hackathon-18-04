package com.example.chat.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Pattern(
        regexp = "^[a-zA-Z0-9_]{3,30}$",
        message = "must be 3–30 characters: letters, digits, or underscores"
    ) String username,
    @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password
) {}
