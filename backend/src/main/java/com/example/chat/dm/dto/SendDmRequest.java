package com.example.chat.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendDmRequest(@NotBlank @Size(max = 3000) String content) {}
