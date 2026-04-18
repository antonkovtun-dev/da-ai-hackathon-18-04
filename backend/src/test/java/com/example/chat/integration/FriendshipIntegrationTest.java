package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.bans.UserBlockRepository;
import com.example.chat.bans.dto.BlockRequest;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendRequestStatus;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.friendships.dto.FriendRequestResponse;
import com.example.chat.friendships.dto.FriendResponse;
import com.example.chat.friendships.dto.SendFriendRequestRequest;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired FriendRequestRepository friendRequestRepository;
    @Autowired FriendshipRepository friendshipRepository;
    @Autowired UserBlockRepository userBlockRepository;

    @BeforeEach
    void cleanup() {
        userBlockRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void full_friend_request_lifecycle() {
        String alice = loginAs("alice@test.com", "alice");
        String bob   = loginAs("bob@test.com", "bob");

        // Alice sends request to Bob
        var sendResp = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(alice, new SendFriendRequestRequest("bob", "hey!")),
            FriendRequestResponse.class);
        assertThat(sendResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID requestId = sendResp.getBody().id();

        // Bob sees incoming request
        var incoming = restTemplate.exchange("/api/friends/requests/incoming",
            HttpMethod.GET, auth(bob), FriendRequestResponse[].class);
        assertThat(incoming.getBody()).hasSize(1);
        assertThat(incoming.getBody()[0].id()).isEqualTo(requestId);

        // Bob accepts
        restTemplate.exchange("/api/friends/requests/" + requestId + "/accept",
            HttpMethod.POST, auth(bob), Void.class);

        // Both see each other as friends
        var aliceFriends = restTemplate.exchange("/api/friends", HttpMethod.GET, auth(alice), FriendResponse[].class);
        assertThat(aliceFriends.getBody()).hasSize(1).extracting(FriendResponse::username).containsExactly("bob");

        var bobFriends = restTemplate.exchange("/api/friends", HttpMethod.GET, auth(bob), FriendResponse[].class);
        assertThat(bobFriends.getBody()).hasSize(1).extracting(FriendResponse::username).containsExactly("alice");
    }

    @Test
    void decline_request_leaves_no_friendship() {
        String alice = loginAs("alice2@test.com", "alice2");
        String bob   = loginAs("bob2@test.com", "bob2");

        var req = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(alice, new SendFriendRequestRequest("bob2", null)),
            FriendRequestResponse.class).getBody();

        restTemplate.exchange("/api/friends/requests/" + req.id() + "/decline",
            HttpMethod.POST, auth(bob), Void.class);

        assertThat(friendshipRepository.count()).isZero();
        assertThat(friendRequestRepository.findById(req.id()).get().getStatus())
            .isEqualTo(FriendRequestStatus.DECLINED);
    }

    @Test
    void cancel_request() {
        String alice = loginAs("alice3@test.com", "alice3");
        loginAs("bob3@test.com", "bob3");

        var req = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(alice, new SendFriendRequestRequest("bob3", null)),
            FriendRequestResponse.class).getBody();

        var cancel = restTemplate.exchange("/api/friends/requests/" + req.id(),
            HttpMethod.DELETE, auth(alice), Void.class);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(friendRequestRepository.findById(req.id()).get().getStatus())
            .isEqualTo(FriendRequestStatus.CANCELED);
    }

    @Test
    void remove_friend() {
        String alice = loginAs("alice4@test.com", "alice4");
        String bob   = loginAs("bob4@test.com", "bob4");

        var req = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(alice, new SendFriendRequestRequest("bob4", null)),
            FriendRequestResponse.class).getBody();
        restTemplate.exchange("/api/friends/requests/" + req.id() + "/accept",
            HttpMethod.POST, auth(bob), Void.class);

        UUID bobUserId = userRepository.findByUsername("bob4").get().getId();

        var remove = restTemplate.exchange("/api/friends/" + bobUserId,
            HttpMethod.DELETE, auth(alice), Void.class);
        assertThat(remove.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(friendshipRepository.count()).isZero();
    }

    @Test
    void block_terminates_friendship_and_prevents_new_request() {
        String alice = loginAs("alice5@test.com", "alice5");
        String bob   = loginAs("bob5@test.com", "bob5");

        // Become friends
        var req = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(alice, new SendFriendRequestRequest("bob5", null)),
            FriendRequestResponse.class).getBody();
        restTemplate.exchange("/api/friends/requests/" + req.id() + "/accept",
            HttpMethod.POST, auth(bob), Void.class);
        assertThat(friendshipRepository.count()).isEqualTo(1);

        // Alice blocks Bob
        UUID bobId = userRepository.findByUsername("bob5").get().getId();
        var blockResp = restTemplate.exchange("/api/blocks", HttpMethod.POST,
            body(alice, new BlockRequest(bobId)), Void.class);
        assertThat(blockResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Friendship removed
        assertThat(friendshipRepository.count()).isZero();

        // Bob cannot send friend request to Alice
        var sendReq = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(bob, new SendFriendRequestRequest("alice5", null)),
            String.class);
        assertThat(sendReq.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
