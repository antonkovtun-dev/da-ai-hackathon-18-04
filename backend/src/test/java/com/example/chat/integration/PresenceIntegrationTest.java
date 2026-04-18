package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.bans.UserBlockRepository;
import com.example.chat.dm.DmMessageRepository;
import com.example.chat.dm.DmThreadRepository;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.messages.MessageRepository;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.presence.PresenceTabRepository;
import com.example.chat.presence.dto.HeartbeatRequest;
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired PresenceTabRepository presenceTabRepository;
    @Autowired RoomBanRepository roomBanRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ReadStateRepository readStateRepository;
    @Autowired RoomMembershipRepository membershipRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired FriendRequestRepository friendRequestRepository;
    @Autowired FriendshipRepository friendshipRepository;
    @Autowired UserBlockRepository userBlockRepository;
    @Autowired DmMessageRepository dmMessageRepository;
    @Autowired DmThreadRepository dmThreadRepository;

    @BeforeEach
    void cleanup() {
        presenceTabRepository.deleteAll();
        readStateRepository.deleteAll();
        messageRepository.deleteAll();
        roomBanRepository.deleteAll();
        membershipRepository.deleteAll();
        roomRepository.deleteAll();
        dmMessageRepository.deleteAll();
        dmThreadRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        userBlockRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void heartbeat_with_active_tab_yields_online_status() {
        String alice = loginAs("alice@test.com", "alice");

        restTemplate.exchange("/api/presence/heartbeat", HttpMethod.POST,
            body(alice, new HeartbeatRequest("tab-1", true)), Void.class);

        var userId = userRepository.findByUsername("alice").get().getId();
        var resp = restTemplate.exchange("/api/presence/" + userId,
            HttpMethod.GET, auth(alice), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("status")).isEqualTo("ONLINE");
    }

    @Test
    void multiple_heartbeats_from_same_tab_are_idempotent() {
        String alice = loginAs("alice2@test.com", "alice2");

        for (int i = 0; i < 3; i++) {
            restTemplate.exchange("/api/presence/heartbeat", HttpMethod.POST,
                body(alice, new HeartbeatRequest("tab-1", true)), Void.class);
        }

        assertThat(presenceTabRepository.count()).isEqualTo(1);
    }

    @Test
    void two_separate_tabs_both_tracked() {
        String alice = loginAs("alice3@test.com", "alice3");

        restTemplate.exchange("/api/presence/heartbeat", HttpMethod.POST,
            body(alice, new HeartbeatRequest("tab-A", true)), Void.class);
        restTemplate.exchange("/api/presence/heartbeat", HttpMethod.POST,
            body(alice, new HeartbeatRequest("tab-B", false)), Void.class);

        assertThat(presenceTabRepository.count()).isEqualTo(2);

        var userId = userRepository.findByUsername("alice3").get().getId();
        var resp = restTemplate.exchange("/api/presence/" + userId,
            HttpMethod.GET, auth(alice), Map.class);
        assertThat(resp.getBody().get("status")).isEqualTo("ONLINE");
    }

    // --- helpers ---

    private String loginAs(String email, String username) {
        restTemplate.postForEntity("/api/auth/register",
            new RegisterRequest(email, username, "password123"), Void.class);
        var resp = restTemplate.postForEntity("/api/auth/login",
            new LoginRequest(email, "password123"), Void.class);
        return resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private HttpEntity<Void> auth(String session) {
        var h = new HttpHeaders();
        h.set(HttpHeaders.COOKIE, session);
        return new HttpEntity<>(h);
    }

    private <T> HttpEntity<T> body(String session, T b) {
        var h = new HttpHeaders();
        h.set(HttpHeaders.COOKIE, session);
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(b, h);
    }
}
