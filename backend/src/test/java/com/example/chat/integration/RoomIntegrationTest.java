package com.example.chat.integration;

import com.example.chat.auth.dto.LoginRequest;
import com.example.chat.auth.dto.RegisterRequest;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.memberships.dto.MemberResponse;
import com.example.chat.messages.MessageRepository;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.moderation.dto.BanRequest;
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

class RoomIntegrationTest extends IntegrationTestBase {

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
    void create_room_joins_creator_as_owner() {
        String session = loginAs("alice@test.com", "alice");
        var resp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(session, new CreateRoomRequest("general", null)), RoomResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().name()).isEqualTo("general");
        assertThat(resp.getBody().memberCount()).isEqualTo(1);

        UUID roomId = resp.getBody().id();
        var members = restTemplate.exchange("/api/rooms/" + roomId + "/members",
                HttpMethod.GET, auth(session), MemberResponse[].class);
        assertThat(members.getBody()).hasSize(1);
        assertThat(members.getBody()[0].role()).isEqualTo(RoomRole.OWNER);
    }

    @Test
    void join_room_adds_member() {
        String ownerSession = loginAs("owner@test.com", "owner");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("dev", null)), RoomResponse.class).getBody();

        String memberSession = loginAs("bob@test.com", "bob");
        var joinResp = restTemplate.exchange("/api/rooms/" + room.id() + "/join",
                HttpMethod.POST, auth(memberSession), Void.class);
        assertThat(joinResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var members = restTemplate.exchange("/api/rooms/" + room.id() + "/members",
                HttpMethod.GET, auth(ownerSession), MemberResponse[].class).getBody();
        assertThat(members).hasSize(2);
    }

    @Test
    void banned_user_cannot_join() {
        String ownerSession = loginAs("owner2@test.com", "owner2");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("secret", null)), RoomResponse.class).getBody();

        String targetSession = loginAs("target@test.com", "target");
        restTemplate.exchange("/api/rooms/" + room.id() + "/join", HttpMethod.POST, auth(targetSession), Void.class);

        var members = restTemplate.exchange("/api/rooms/" + room.id() + "/members",
                HttpMethod.GET, auth(ownerSession), MemberResponse[].class).getBody();
        UUID targetId = members[1].userId();

        restTemplate.exchange("/api/rooms/" + room.id() + "/bans", HttpMethod.POST,
                body(ownerSession, new BanRequest(targetId, "test")), Void.class);

        var rejoin = restTemplate.exchange("/api/rooms/" + room.id() + "/join",
                HttpMethod.POST, auth(targetSession), Void.class);
        assertThat(rejoin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void member_cannot_ban() {
        String ownerSession = loginAs("owner3@test.com", "owner3");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("lobby", null)), RoomResponse.class).getBody();

        String memberSession = loginAs("member@test.com", "member");
        restTemplate.exchange("/api/rooms/" + room.id() + "/join", HttpMethod.POST, auth(memberSession), Void.class);

        var banResp = restTemplate.exchange("/api/rooms/" + room.id() + "/bans", HttpMethod.POST,
                body(memberSession, new BanRequest(UUID.randomUUID(), null)), Void.class);
        assertThat(banResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void non_member_cannot_get_members() {
        String ownerSession = loginAs("owner4@test.com", "owner4");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("private", null)), RoomResponse.class).getBody();

        String outsiderSession = loginAs("outsider@test.com", "outsider");
        var resp = restTemplate.exchange("/api/rooms/" + room.id() + "/members",
                HttpMethod.GET, auth(outsiderSession), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void owner_can_delete_room() {
        String ownerSession = loginAs("owner5@test.com", "owner5");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("temp", null)), RoomResponse.class).getBody();

        var del = restTemplate.exchange("/api/rooms/" + room.id(), HttpMethod.DELETE, auth(ownerSession), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(roomRepository.findById(room.id())).isEmpty();
    }

    @Test
    void member_cannot_delete_room() {
        String ownerSession = loginAs("owner6@test.com", "owner6");
        var room = restTemplate.exchange("/api/rooms", HttpMethod.POST,
                body(ownerSession, new CreateRoomRequest("keep", null)), RoomResponse.class).getBody();

        String memberSession = loginAs("member2@test.com", "member2");
        restTemplate.exchange("/api/rooms/" + room.id() + "/join", HttpMethod.POST, auth(memberSession), Void.class);

        var del = restTemplate.exchange("/api/rooms/" + room.id(), HttpMethod.DELETE, auth(memberSession), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
