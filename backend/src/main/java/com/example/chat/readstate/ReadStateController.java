package com.example.chat.readstate;

import com.example.chat.auth.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class ReadStateController {

    private final ReadStateService readStateService;

    public ReadStateController(ReadStateService readStateService) {
        this.readStateService = readStateService;
    }

    @GetMapping("/unread")
    public Map<UUID, Long> getUnread() {
        return readStateService.getUnreadCounts(currentUserId());
    }

    @PostMapping("/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID roomId) {
        readStateService.markRead(roomId, currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal()).getUserId();
    }
}
