package com.example.chat.dm.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateThreadRequest(@NotNull UUID targetUserId) {}
