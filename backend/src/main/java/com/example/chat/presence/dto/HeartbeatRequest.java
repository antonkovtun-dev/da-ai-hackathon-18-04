package com.example.chat.presence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HeartbeatRequest(
    @NotBlank @Size(max = 64) String tabId,
    boolean active
) {}
