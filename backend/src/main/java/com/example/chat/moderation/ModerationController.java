package com.example.chat.moderation;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.moderation.dto.BanRequest;
import com.example.chat.moderation.dto.BanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/bans")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) { this.moderationService = moderationService; }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ban(@PathVariable UUID roomId, @Valid @RequestBody BanRequest req) {
        moderationService.banUser(roomId, req.userId(), currentUserId(), req.reason());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unban(@PathVariable UUID roomId, @PathVariable UUID userId) {
        moderationService.unbanUser(roomId, userId, currentUserId());
    }

    @GetMapping
    public List<BanResponse> listBans(@PathVariable UUID roomId) {
        return moderationService.listBans(roomId, currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
