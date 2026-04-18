package com.example.chat.messages.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id, UUID roomId, UUID authorId, String authorUsername,
    String content, OffsetDateTime createdAt, OffsetDateTime editedAt, boolean deleted
) {}
