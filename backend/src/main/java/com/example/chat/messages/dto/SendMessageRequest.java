package com.example.chat.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(@NotBlank @Size(max = 3000) String content) {}
