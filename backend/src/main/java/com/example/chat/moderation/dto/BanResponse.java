package com.example.chat.moderation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BanResponse(UUID id, UUID userId, String username, UUID bannedBy, String reason, OffsetDateTime bannedAt) {}
