package com.example.chat.dm;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.dm.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dm")
public class DmController {

    private final DmService dmService;

    public DmController(DmService dmService) {
        this.dmService = dmService;
    }

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public DmThreadResponse getOrCreateThread(@Valid @RequestBody CreateThreadRequest req) {
        return dmService.getOrCreateThread(currentUserId(), req.targetUserId());
    }

    @GetMapping("/threads")
    public List<DmThreadResponse> listThreads() {
        return dmService.listThreads(currentUserId());
    }

    @GetMapping("/threads/{threadId}/messages")
    public List<DmMessageResponse> getMessages(
            @PathVariable UUID threadId,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "50") int limit) {
        return dmService.getMessages(currentUserId(), threadId, before, limit);
    }

    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public DmMessageResponse sendMessage(@PathVariable UUID threadId,
                                         @Valid @RequestBody SendDmRequest req) {
        return dmService.sendMessage(currentUserId(), threadId, req);
    }

    @PatchMapping("/messages/{id}")
    public DmMessageResponse editMessage(@PathVariable UUID id,
                                          @Valid @RequestBody EditDmRequest req) {
        return dmService.editMessage(currentUserId(), id, req.content());
    }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID id) {
        dmService.deleteMessage(currentUserId(), id);
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
