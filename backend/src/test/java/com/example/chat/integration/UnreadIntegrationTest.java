package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.messages.dto.SendMessageRequest;
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.rooms.dto.CreateRoomRequest;
import com.example.chat.rooms.dto.RoomResponse;
import com.example.chat.memberships.RoomMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UnreadIntegrationTest extends IntegrationTestBase {

    @Autowired RoomRepository roomRepository;
    @Autowired RoomMembershipRepository membershipRepository;
    @Autowired ReadStateRepository readStateRepository;

    @BeforeEach
    void cleanup() {
        readStateRepository.deleteAll();
        membershipRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void unread_count_increments_for_non_sender_and_clears_on_mark_read() {
        String alice = loginAs("alice@test.com", "alice");
        String bob   = loginAs("bob@test.com", "bob");

        // Alice creates a room
        RoomResponse room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, new CreateRoomRequest("testroom", null)),
            RoomResponse.class).getBody();
        UUID roomId = room.id();

        // Bob joins
        restTemplate.exchange("/api/rooms/" + roomId + "/join",
            HttpMethod.POST, auth(bob), Void.class);

        // Alice sends a message (marks Alice as read automatically)
        restTemplate.exchange("/api/rooms/" + roomId + "/messages", HttpMethod.POST,
            body(alice, new SendMessageRequest("hello")),
            Void.class);

        // Alice's unread for the room = 0 (she sent it, so she's marked read)
        var aliceUnread = restTemplate.exchange("/api/rooms/unread",
            HttpMethod.GET, auth(alice), Map.class);
        assertThat((Integer) aliceUnread.getBody().get(roomId.toString())).isZero();

        // Bob's unread for the room = 1
        var bobUnread = restTemplate.exchange("/api/rooms/unread",
            HttpMethod.GET, auth(bob), Map.class);
        assertThat((Integer) bobUnread.getBody().get(roomId.toString())).isEqualTo(1);

        // Bob marks room as read
        restTemplate.exchange("/api/rooms/" + roomId + "/read",
            HttpMethod.POST, auth(bob), Void.class);

        // Bob's unread now = 0
        var bobUnreadAfter = restTemplate.exchange("/api/rooms/unread",
            HttpMethod.GET, auth(bob), Map.class);
        assertThat((Integer) bobUnreadAfter.getBody().get(roomId.toString())).isZero();
    }

    @Test
    void second_message_increments_count_again() {
        String alice = loginAs("alice2@test.com", "alice2");
        String bob   = loginAs("bob2@test.com", "bob2");

        RoomResponse room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, new CreateRoomRequest("testroom2", null)),
            RoomResponse.class).getBody();
        UUID roomId = room.id();

        restTemplate.exchange("/api/rooms/" + roomId + "/join",
            HttpMethod.POST, auth(bob), Void.class);

        // Alice sends 3 messages
        for (int i = 0; i < 3; i++) {
            restTemplate.exchange("/api/rooms/" + roomId + "/messages", HttpMethod.POST,
                body(alice, new SendMessageRequest("msg " + i)), Void.class);
        }

        var bobUnread = restTemplate.exchange("/api/rooms/unread",
            HttpMethod.GET, auth(bob), Map.class);
        assertThat((Integer) bobUnread.getBody().get(roomId.toString())).isEqualTo(3);
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
