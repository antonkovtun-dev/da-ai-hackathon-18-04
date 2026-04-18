package com.example.chat.friendships.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FriendResponse(UUID userId, String username, OffsetDateTime friendsSince) {}
