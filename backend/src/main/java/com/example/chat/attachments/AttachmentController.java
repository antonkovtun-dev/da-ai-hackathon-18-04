package com.example.chat.attachments;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.messages.dto.MessageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/api/rooms/{roomId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse upload(
            @PathVariable UUID roomId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "comment", required = false) String comment) {
        return attachmentService.upload(roomId, currentUserId(), file, comment);
    }

    @GetMapping("/api/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        return attachmentService.download(id, currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getUserId();
    }
}
