package com.example.chat.bans.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BlockedUserResponse(UUID userId, String username, OffsetDateTime blockedAt) {}
