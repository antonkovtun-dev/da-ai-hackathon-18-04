package com.example.chat.messages.dto;

import com.example.chat.attachments.dto.AttachmentResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id, UUID roomId, UUID authorId, String authorUsername,
    String content, OffsetDateTime createdAt, OffsetDateTime editedAt, boolean deleted,
    AttachmentResponse attachment
) {}
