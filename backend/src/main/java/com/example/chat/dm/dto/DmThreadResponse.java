package com.example.chat.dm.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DmThreadResponse(UUID id, UUID otherUserId, String otherUsername, OffsetDateTime createdAt) {}
