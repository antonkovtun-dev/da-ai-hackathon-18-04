package com.example.chat.attachments.dto;

import java.util.UUID;

public record AttachmentResponse(UUID id, String filename, String contentType, long size) {}
