package com.example.chat.bans.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BlockRequest(@NotNull UUID targetUserId) {}
