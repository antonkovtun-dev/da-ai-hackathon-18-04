package com.example.chat.rooms.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomResponse(UUID id, String name, String description, UUID ownerId, long memberCount, OffsetDateTime createdAt) {}
