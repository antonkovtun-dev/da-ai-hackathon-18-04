package com.example.chat.dm.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DmMessageResponse(
    UUID id,
    UUID threadId,
    UUID authorId,
    String authorUsername,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime editedAt,
    boolean deleted
) {}
