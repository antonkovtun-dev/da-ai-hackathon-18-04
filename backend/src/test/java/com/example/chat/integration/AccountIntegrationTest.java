package com.example.chat.integration;

import com.example.chat.attachments.AttachmentRepository;
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
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountIntegrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbcTemplate;
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
        jdbcTemplate.execute("DELETE FROM password_reset_tokens");
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
    void deleteAccount_invalidates_session_and_prevents_login() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("alice@example.com", "alice", "password123"), Void.class);
        var loginResp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("alice@example.com", "password123"), Void.class);
        String session = sessionCookie(loginResp);

        var deleteResp = restTemplate.exchange("/api/users/me", HttpMethod.DELETE,
                withSession(session), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var meResp = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                withSession(session), Void.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var reLoginResp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("alice@example.com", "password123"), Void.class);
        assertThat(reLoginResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePassword_allows_login_with_new_password() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("bob@example.com", "bob", "oldpass12"), Void.class);
        var loginResp = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("bob@example.com", "oldpass12"), Void.class);
        String session = sessionCookie(loginResp);

        var change = restTemplate.exchange("/api/users/me/password", HttpMethod.PUT,
                withSessionAndBody(session, Map.of("currentPassword", "oldpass12", "newPassword", "newpass123")),
                Void.class);
        assertThat(change.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var old = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("bob@example.com", "oldpass12"), Void.class);
        assertThat(old.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var newLogin = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("bob@example.com", "newpass123"), Void.class);
        assertThat(newLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPassword_logs_token_and_reset_changes_password() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("carol@example.com", "carol", "password123"), Void.class);

        var forgotResp = restTemplate.postForEntity("/api/auth/forgot-password",
                Map.of("email", "carol@example.com"), Void.class);
        assertThat(forgotResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String token = jdbcTemplate.queryForObject(
                "SELECT token FROM password_reset_tokens WHERE user_id = " +
                "(SELECT id FROM users WHERE email = 'carol@example.com')",
                String.class);
        assertThat(token).isNotNull();

        var resetResp = restTemplate.postForEntity("/api/auth/reset-password",
                Map.of("token", token, "newPassword", "newpass123"), Void.class);
        assertThat(resetResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("carol@example.com", "password123"), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("carol@example.com", "newpass123"), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- helpers ---

    private String sessionCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (setCookie == null) return null;
        return setCookie.split(";")[0];
    }

    private HttpEntity<Void> withSession(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> withSessionAndBody(String sessionCookie, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
