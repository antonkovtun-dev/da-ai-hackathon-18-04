package com.example.chat.messages;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.messages.dto.EditMessageRequest;
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.messages.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) { this.messageService = messageService; }

    @GetMapping("/api/rooms/{roomId}/messages")
    public List<MessageResponse> list(
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "50") int limit) {
        return messageService.getMessages(roomId, currentUserId(), before, Math.min(limit, 100));
    }

    @PostMapping("/api/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable UUID roomId,
                                @Valid @RequestBody SendMessageRequest req) {
        return messageService.sendMessage(roomId, currentUserId(), req.content());
    }

    @PatchMapping("/api/messages/{id}")
    public MessageResponse edit(@PathVariable UUID id,
                                @Valid @RequestBody EditMessageRequest req) {
        return messageService.editMessage(id, currentUserId(), req.content());
    }

    @DeleteMapping("/api/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        messageService.deleteMessage(id, currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
