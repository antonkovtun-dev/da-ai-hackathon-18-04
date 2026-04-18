package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.messages.MessageRepository;
import com.example.chat.messages.dto.EditMessageRequest;
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.messages.dto.SendMessageRequest;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.rooms.dto.CreateRoomRequest;
import com.example.chat.rooms.dto.RoomResponse;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired RoomMembershipRepository membershipRepository;
    @Autowired RoomBanRepository banRepository;
    @Autowired MessageRepository messageRepository;

    @BeforeEach
    void cleanup() {
        messageRepository.deleteAll();
        banRepository.deleteAll();
        membershipRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void send_message_returns_message_with_author() {
        String session = loginAs("alice@test.com", "alice");
        UUID roomId = createRoom(session, "chat");

        var resp = restTemplate.exchange("/api/rooms/" + roomId + "/messages", HttpMethod.POST,
                body(session, new SendMessageRequest("hello world")), MessageResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().content()).isEqualTo("hello world");
        assertThat(resp.getBody().authorUsername()).isEqualTo("alice");
        assertThat(resp.getBody().deleted()).isFalse();
    }

    @Test
    void non_member_cannot_send_message() {
        String ownerSession = loginAs("owner@test.com", "owner");
        UUID roomId = createRoom(ownerSession, "restricted");

        String outsiderSession = loginAs("bob@test.com", "bob");
        var resp = restTemplate.exchange("/api/rooms/" + roomId + "/messages", HttpMethod.POST,
                body(outsiderSession, new SendMessageRequest("hi")), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void edit_own_message() {
        String session = loginAs("alice2@test.com", "alice2");
        UUID roomId = createRoom(session, "chat2");
        var msg = sendMessage(session, roomId, "original");

        var editResp = restTemplate.exchange("/api/messages/" + msg.id(), HttpMethod.PATCH,
                body(session, new EditMessageRequest("updated")), MessageResponse.class);
        assertThat(editResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editResp.getBody().content()).isEqualTo("updated");
        assertThat(editResp.getBody().editedAt()).isNotNull();
    }

    @Test
    void cannot_edit_others_message() {
        String aliceSession = loginAs("alice3@test.com", "alice3");
        UUID roomId = createRoom(aliceSession, "chat3");
        var msg = sendMessage(aliceSession, roomId, "alice said this");

        String bobSession = loginAs("bob3@test.com", "bob3");
        restTemplate.exchange("/api/rooms/" + roomId + "/join", HttpMethod.POST, auth(bobSession), Void.class);

        var editResp = restTemplate.exchange("/api/messages/" + msg.id(), HttpMethod.PATCH,
                body(bobSession, new EditMessageRequest("bob changed it")), Void.class);
        assertThat(editResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_own_message_soft_deletes() {
        String session = loginAs("alice4@test.com", "alice4");
        UUID roomId = createRoom(session, "chat4");
        var msg = sendMessage(session, roomId, "delete me");

        restTemplate.exchange("/api/messages/" + msg.id(), HttpMethod.DELETE, auth(session), Void.class);

        var history = restTemplate.exchange("/api/rooms/" + roomId + "/messages",
                HttpMethod.GET, auth(session), MessageResponse[].class).getBody();
        assertThat(history).hasSize(1);
        assertThat(history[0].content()).isNull();
        assertThat(history[0].deleted()).isTrue();
    }

    @Test
    void cursor_pagination_returns_correct_order() throws InterruptedException {
        String session = loginAs("alice5@test.com", "alice5");
        UUID roomId = createRoom(session, "chat5");
        sendMessage(session, roomId, "msg1");
        Thread.sleep(10); // ensure distinct timestamps
        sendMessage(session, roomId, "msg2");
        Thread.sleep(10);
        sendMessage(session, roomId, "msg3");

        var page1 = restTemplate.exchange("/api/rooms/" + roomId + "/messages?limit=2",
                HttpMethod.GET, auth(session), MessageResponse[].class).getBody();
        assertThat(page1).hasSize(2);
        assertThat(page1[0].content()).isEqualTo("msg3");
        assertThat(page1[1].content()).isEqualTo("msg2");

        var oldestId = page1[1].id();
        var page2 = restTemplate.exchange("/api/rooms/" + roomId + "/messages?limit=2&before=" + oldestId,
                HttpMethod.GET, auth(session), MessageResponse[].class).getBody();
        assertThat(page2).hasSize(1);
        assertThat(page2[0].content()).isEqualTo("msg1");
    }

    // --- helpers ---

    private String loginAs(String email, String username) {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest(email, username, "password123"), Void.class);
        var resp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest(email, "password123"), Void.class);
        return resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private UUID createRoom(String session, String name) {
        var resp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(session, new CreateRoomRequest(name, null)), RoomResponse.class);
        return resp.getBody().id();
    }

    private MessageResponse sendMessage(String session, UUID roomId, String content) {
        return restTemplate.exchange("/api/rooms/" + roomId + "/messages", HttpMethod.POST,
                body(session, new SendMessageRequest(content)), MessageResponse.class).getBody();
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
