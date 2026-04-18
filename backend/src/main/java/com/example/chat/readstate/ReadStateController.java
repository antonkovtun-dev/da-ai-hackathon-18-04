package com.example.chat.readstate;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.memberships.RoomMembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class ReadStateController {

    private final ReadStateService readStateService;
    private final RoomMembershipRepository membershipRepository;

    public ReadStateController(ReadStateService readStateService,
                               RoomMembershipRepository membershipRepository) {
        this.readStateService = readStateService;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/unread")
    public Map<UUID, Long> getUnread() {
        return readStateService.getUnreadCounts(currentUserId());
    }

    @PostMapping("/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID roomId) {
        UUID userId = currentUserId();
        if (!membershipRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        readStateService.markRead(roomId, userId);
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal()).getUserId();
    }
}
