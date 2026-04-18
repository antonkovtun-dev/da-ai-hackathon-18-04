package com.example.chat.friendships.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendFriendRequestRequest(
    @NotBlank @Size(max = 30) String targetUsername,
    @Size(max = 200) String message
) {}
