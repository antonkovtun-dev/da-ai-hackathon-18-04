package com.example.chat.memberships.dto;

import com.example.chat.memberships.RoomRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(UUID userId, String username, RoomRole role, OffsetDateTime joinedAt) {}
