package com.example.chat.friendships.dto;

import com.example.chat.friendships.FriendRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FriendRequestResponse(
    UUID id,
    UUID senderId,
    String senderUsername,
    UUID receiverId,
    String receiverUsername,
    String message,
    FriendRequestStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
