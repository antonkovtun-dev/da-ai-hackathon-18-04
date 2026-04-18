package com.example.chat.presence;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.presence.dto.HeartbeatRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@RequestBody @jakarta.validation.Valid HeartbeatRequest req) {
        presenceService.heartbeat(currentUserId(), req.tabId(), req.active());
    }

    @GetMapping("/{userId}")
    public Map<String, String> getStatus(@PathVariable UUID userId) {
        return Map.of("status", presenceService.getStatus(userId));
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal()).getUserId();
    }
}
