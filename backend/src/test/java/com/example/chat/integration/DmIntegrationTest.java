package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.attachments.AttachmentRepository;
import com.example.chat.bans.UserBlockRepository;
import com.example.chat.bans.dto.BlockRequest;
import com.example.chat.dm.DmMessageRepository;
import com.example.chat.dm.DmThreadRepository;
import com.example.chat.dm.dto.CreateThreadRequest;
import com.example.chat.dm.dto.DmMessageResponse;
import com.example.chat.dm.dto.DmThreadResponse;
import com.example.chat.dm.dto.SendDmRequest;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.friendships.dto.FriendRequestResponse;
import com.example.chat.friendships.dto.SendFriendRequestRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DmIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired FriendRequestRepository friendRequestRepository;
    @Autowired FriendshipRepository friendshipRepository;
    @Autowired UserBlockRepository userBlockRepository;
    @Autowired DmMessageRepository dmMessageRepository;
    @Autowired DmThreadRepository dmThreadRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired PresenceTabRepository presenceTabRepository;
    @Autowired ReadStateRepository readStateRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired RoomBanRepository roomBanRepository;
    @Autowired RoomMembershipRepository membershipRepository;
    @Autowired RoomRepository roomRepository;

    @BeforeEach
    void cleanup() {
        attachmentRepository.deleteAll();
        presenceTabRepository.deleteAll();
        readStateRepository.deleteAll();
        dmMessageRepository.deleteAll();
        dmThreadRepository.deleteAll();
        messageRepository.deleteAll();
        roomBanRepository.deleteAll();
        membershipRepository.deleteAll();
        roomRepository.deleteAll();
        userBlockRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void friends_can_send_and_receive_dm() {
        String alice = loginAs("alice@dm.com", "alicedm");
        String bob   = loginAs("bob@dm.com", "bobdm");
        makeFriends(alice, bob, "alicedm", "bobdm");

        UUID bobId = userRepository.findByUsername("bobdm").get().getId();

        // Alice creates DM thread with Bob
        var thread = restTemplate.exchange("/api/dm/threads", HttpMethod.POST,
            body(alice, new CreateThreadRequest(bobId)),
            DmThreadResponse.class).getBody();
        assertThat(thread).isNotNull();
        assertThat(thread.otherUsername()).isEqualTo("bobdm");

        // Alice sends a message
        var msgResp = restTemplate.exchange(
            "/api/dm/threads/" + thread.id() + "/messages", HttpMethod.POST,
            body(alice, new SendDmRequest("hello bob")),
            DmMessageResponse.class);
        assertThat(msgResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(msgResp.getBody().content()).isEqualTo("hello bob");

        // Bob reads messages
        var msgs = restTemplate.exchange(
            "/api/dm/threads/" + thread.id() + "/messages",
            HttpMethod.GET, auth(bob), DmMessageResponse[].class);
        assertThat(msgs.getBody()).hasSize(1);
        assertThat(msgs.getBody()[0].content()).isEqualTo("hello bob");
    }

    @Test
    void non_friends_cannot_start_dm() {
        String alice = loginAs("alice2@dm.com", "alicedm2");
        loginAs("bob2@dm.com", "bobdm2");

        UUID bobId = userRepository.findByUsername("bobdm2").get().getId();
        var resp = restTemplate.exchange("/api/dm/threads", HttpMethod.POST,
            body(alice, new CreateThreadRequest(bobId)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void after_unfriend_cannot_send_new_dm() {
        String alice = loginAs("alice3@dm.com", "alicedm3");
        String bob   = loginAs("bob3@dm.com", "bobdm3");
        makeFriends(alice, bob, "alicedm3", "bobdm3");

        UUID bobId = userRepository.findByUsername("bobdm3").get().getId();
        var thread = restTemplate.exchange("/api/dm/threads", HttpMethod.POST,
            body(alice, new CreateThreadRequest(bobId)),
            DmThreadResponse.class).getBody();

        // Alice removes Bob as friend
        restTemplate.exchange("/api/friends/" + bobId, HttpMethod.DELETE, auth(alice), Void.class);

        // Alice can no longer send DMs (thread exists but eligibility fails)
        var sendResp = restTemplate.exchange(
            "/api/dm/threads/" + thread.id() + "/messages", HttpMethod.POST,
            body(alice, new SendDmRequest("still here?")),
            String.class);
        assertThat(sendResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void blocked_user_cannot_send_dm() {
        String alice = loginAs("alice4@dm.com", "alicedm4");
        String bob   = loginAs("bob4@dm.com", "bobdm4");
        makeFriends(alice, bob, "alicedm4", "bobdm4");

        UUID bobId = userRepository.findByUsername("bobdm4").get().getId();
        var thread = restTemplate.exchange("/api/dm/threads", HttpMethod.POST,
            body(alice, new CreateThreadRequest(bobId)),
            DmThreadResponse.class).getBody();

        // Alice blocks Bob (removes friendship, freezes DM)
        restTemplate.exchange("/api/blocks", HttpMethod.POST,
            body(alice, new BlockRequest(bobId)), Void.class);

        // Bob cannot send DMs to Alice
        var sendResp = restTemplate.exchange(
            "/api/dm/threads/" + thread.id() + "/messages", HttpMethod.POST,
            body(bob, new SendDmRequest("can you hear me?")),
            String.class);
        assertThat(sendResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- helpers ---

    private void makeFriends(String aliceSession, String bobSession, String aliceUsername, String bobUsername) {
        var req = restTemplate.exchange("/api/friends/requests", HttpMethod.POST,
            body(aliceSession, new SendFriendRequestRequest(bobUsername, null)),
            FriendRequestResponse.class).getBody();
        restTemplate.exchange("/api/friends/requests/" + req.id() + "/accept",
            HttpMethod.POST, auth(bobSession), Void.class);
    }

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
