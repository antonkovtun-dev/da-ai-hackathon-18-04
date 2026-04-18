package com.example.chat.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditDmRequest(@NotBlank @Size(max = 3000) String content) {}
