package com.example.chat.bans;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.bans.dto.BlockRequest;
import com.example.chat.bans.dto.BlockedUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocks")
public class UserBlockController {

    private final UserBlockService userBlockService;

    public UserBlockController(UserBlockService userBlockService) {
        this.userBlockService = userBlockService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@Valid @RequestBody BlockRequest req) {
        userBlockService.blockUser(currentUserId(), req);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable UUID userId) {
        userBlockService.unblockUser(currentUserId(), userId);
    }

    @GetMapping
    public List<BlockedUserResponse> listBlocked() {
        return userBlockService.listBlocked(currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
