package com.example.chat.integration;

import com.example.chat.attachments.AttachmentRepository;
import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.auth.dto.UserResponse;
import com.example.chat.bans.UserBlockRepository;
import com.example.chat.dm.DmMessageRepository;
import com.example.chat.dm.DmThreadRepository;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.messages.MessageRepository;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.presence.PresenceTabRepository;
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired PresenceTabRepository presenceTabRepository;
    @Autowired ReadStateRepository readStateRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired RoomBanRepository roomBanRepository;
    @Autowired RoomMembershipRepository membershipRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired DmMessageRepository dmMessageRepository;
    @Autowired DmThreadRepository dmThreadRepository;
    @Autowired FriendRequestRepository friendRequestRepository;
    @Autowired FriendshipRepository friendshipRepository;
    @Autowired UserBlockRepository userBlockRepository;

    @BeforeEach
    void cleanup() {
        attachmentRepository.deleteAll();
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
    void register_returns_user_without_password_hash() {
        var resp = restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("alice@example.com", "alice", "password123"),
                UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().email()).isEqualTo("alice@example.com");
        assertThat(resp.getBody().username()).isEqualTo("alice");
        assertThat(resp.getBody().id()).isNotNull();
        assertThat(resp.getBody().createdAt()).isNotNull();
    }

    @Test
    void register_sets_session_cookie_so_me_works_immediately() {
        var registerResp = restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("bob@example.com", "bob", "password123"),
                UserResponse.class);

        String session = sessionCookie(registerResp);
        assertThat(session).isNotNull();

        var meResp = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                withSession(session), UserResponse.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody().email()).isEqualTo("bob@example.com");
    }

    @Test
    void register_duplicate_email_returns_422() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("charlie@example.com", "charlie", "password123"), Void.class);

        var resp = restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("charlie@example.com", "charlie2", "password123"), List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void register_duplicate_username_returns_422() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("dave@example.com", "dave", "password123"), Void.class);

        var resp = restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("dave2@example.com", "dave", "password123"), List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void login_returns_user_and_sets_session() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("eve@example.com", "eve", "password123"), Void.class);

        var loginResp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("eve@example.com", "password123"), UserResponse.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody().email()).isEqualTo("eve@example.com");
        assertThat(sessionCookie(loginResp)).isNotNull();
    }

    @Test
    void login_wrong_password_returns_401() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("frank@example.com", "frank", "correctpass"), Void.class);

        var resp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("frank@example.com", "wrongpass"), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_without_session_returns_401() {
        var resp = restTemplate.getForEntity("/api/auth/me", Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_invalidates_session() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("grace@example.com", "grace", "password123"), Void.class);

        var loginResp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("grace@example.com", "password123"), UserResponse.class);
        String session = sessionCookie(loginResp);

        // Confirm session works
        var meResp = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                withSession(session), UserResponse.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Logout
        var logoutResp = restTemplate.exchange("/api/auth/logout", HttpMethod.POST,
                withSession(session), Void.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // /me after logout → 401
        var meAfterLogout = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                withSession(session), Void.class);
        assertThat(meAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private String sessionCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (setCookie == null) return null;
        return setCookie.split(";")[0]; // "SESSION=<id>"
    }

    private HttpEntity<Void> withSession(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        return new HttpEntity<>(headers);
    }
}
