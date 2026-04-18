package com.example.chat.presence;

import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PresenceService {

    private final PresenceTabRepository presenceTabRepository;
    private final RoomMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher roomEventPublisher;

    public PresenceService(PresenceTabRepository presenceTabRepository,
                           RoomMembershipRepository membershipRepository,
                           UserRepository userRepository,
                           RoomEventPublisher roomEventPublisher) {
        this.presenceTabRepository = presenceTabRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Transactional
    public String heartbeat(UUID userId, String tabId, boolean active) {
        PresenceTab tab = presenceTabRepository.findByUserIdAndTabId(userId, tabId)
            .orElseGet(() -> {
                PresenceTab t = new PresenceTab();
                t.setUserId(userId);
                t.setTabId(tabId);
                t.setLastActivityAt(OffsetDateTime.now());
                return t;
            });

        tab.setLastHeartbeatAt(OffsetDateTime.now());
        if (active) tab.setLastActivityAt(OffsetDateTime.now());
        presenceTabRepository.save(tab);

        presenceTabRepository.deleteStale(userId, OffsetDateTime.now().minusMinutes(2));

        String status = deriveStatus(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<UUID> roomIds = membershipRepository.findRoomIdsByUserId(userId);
        for (UUID roomId : roomIds) {
            roomEventPublisher.publishPresenceUpdate(roomId, userId, user.getUsername(), status);
        }

        return status;
    }

    @Transactional(readOnly = true)
    public String getStatus(UUID userId) {
        return deriveStatus(userId);
    }

    private String deriveStatus(UUID userId) {
        List<PresenceTab> tabs = presenceTabRepository.findByUserId(userId);
        if (tabs.isEmpty()) return "OFFLINE";
        OffsetDateTime activeThreshold = OffsetDateTime.now().minusMinutes(1);
        boolean anyActive = tabs.stream()
            .anyMatch(t -> t.getLastActivityAt().isAfter(activeThreshold));
        return anyActive ? "ONLINE" : "AFK";
    }
}
