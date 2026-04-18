package com.example.chat.moderation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BanRequest(@NotNull UUID userId, String reason) {}
