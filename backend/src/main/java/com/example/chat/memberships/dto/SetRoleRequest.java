package com.example.chat.memberships.dto;

import com.example.chat.memberships.RoomRole;
import jakarta.validation.constraints.NotNull;

public record SetRoleRequest(@NotNull RoomRole role) {}
