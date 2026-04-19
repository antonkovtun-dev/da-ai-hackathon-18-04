package com.example.chat.sessions;

import com.example.chat.auth.AuthUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionService.SessionInfo> list(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String currentId = session != null ? session.getId() : null;
        return sessionService.listSessions(currentUserId(), currentId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String sessionId) {
        sessionService.revokeSession(currentUserId(), sessionId);
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getUserId();
    }
}
