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
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.presence.PresenceTabRepository;
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.rooms.dto.RoomResponse;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentIntegrationTest extends IntegrationTestBase {

    @Autowired AttachmentRepository attachmentRepository;
    @Autowired UserRepository userRepository;
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

    // --- tests ---

    @Test
    void member_can_upload_file_and_download_it() {
        String alice = loginAs("alice@test.com", "alice");

        // Create a public room
        var createResp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, Map.of("name", "test-room", "isPrivate", false)), RoomResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID roomId = createResp.getBody().id();

        // Upload a small PNG
        byte[] fileContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        var uploadResp = uploadFile(alice, roomId, "test.png", fileContent, "image/png");

        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var msgBody = uploadResp.getBody();
        assertThat(msgBody).isNotNull();
        assertThat(msgBody.attachment()).isNotNull();
        assertThat(msgBody.attachment().id()).isNotNull();
        assertThat(msgBody.attachment().filename()).isEqualTo("test.png");
        assertThat(msgBody.attachment().size()).isGreaterThan(0);

        // Download the attachment
        UUID attachmentId = msgBody.attachment().id();
        var downloadResp = restTemplate.exchange("/api/attachments/" + attachmentId,
            HttpMethod.GET, auth(alice), byte[].class);

        assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .contains("attachment");
    }

    @Test
    void non_member_cannot_download_attachment() {
        String alice = loginAs("alice2@test.com", "alice2");
        String bob = loginAs("bob2@test.com", "bob2");

        // Alice creates a room and uploads a file
        var createResp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, Map.of("name", "alice-room", "isPrivate", false)), RoomResponse.class);
        UUID roomId = createResp.getBody().id();

        byte[] fileContent = new byte[]{1, 2, 3, 4};
        var uploadResp = uploadFile(alice, roomId, "secret.txt", fileContent, "text/plain");
        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID attachmentId = uploadResp.getBody().attachment().id();

        // Bob (not a member) tries to download
        var downloadResp = restTemplate.exchange("/api/attachments/" + attachmentId,
            HttpMethod.GET, auth(bob), byte[].class);

        assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void non_member_cannot_upload_to_room() {
        String alice = loginAs("alice3@test.com", "alice3");
        String bob = loginAs("bob3@test.com", "bob3");

        // Alice creates a room (bob is NOT a member)
        var createResp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, Map.of("name", "alice-only-room", "isPrivate", false)), RoomResponse.class);
        UUID roomId = createResp.getBody().id();

        // Bob tries to upload a file to alice's room
        byte[] fileContent = new byte[]{1, 2, 3, 4};
        var uploadResp = uploadFile(bob, roomId, "intruder.txt", fileContent, "text/plain");

        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void uploading_oversized_image_returns_400() {
        String alice = loginAs("alice4@test.com", "alice4");

        // Alice creates a room
        var createResp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, Map.of("name", "size-test-room", "isPrivate", false)), RoomResponse.class);
        UUID roomId = createResp.getBody().id();

        // Upload a 4 MB image (exceeds 3 MB limit for images)
        byte[] bigContent = new byte[4 * 1024 * 1024];
        var uploadResp = uploadFile(alice, roomId, "big.jpg", bigContent, "image/jpeg");

        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploaded_attachment_appears_in_message_list() {
        String alice = loginAs("alice5@test.com", "alice5");

        // Alice creates a room and uploads a file
        var createResp = restTemplate.exchange("/api/rooms", HttpMethod.POST,
            body(alice, Map.of("name", "msg-list-room", "isPrivate", false)), RoomResponse.class);
        UUID roomId = createResp.getBody().id();

        byte[] fileContent = new byte[]{10, 20, 30};
        var uploadResp = uploadFile(alice, roomId, "doc.txt", fileContent, "text/plain");
        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Get messages and check attachment is present
        var msgsResp = restTemplate.exchange("/api/rooms/" + roomId + "/messages",
            HttpMethod.GET, auth(alice), MessageResponse[].class);

        assertThat(msgsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(msgsResp.getBody()).isNotNull();
        boolean hasAttachment = Arrays.stream(msgsResp.getBody())
            .anyMatch(m -> m.attachment() != null);
        assertThat(hasAttachment).isTrue();
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

    private ResponseEntity<MessageResponse> uploadFile(String session, UUID roomId,
            String filename, byte[] content, String contentType) {
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, session);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        fileHeaders.setContentDispositionFormData("file", filename);
        var filePart = new HttpEntity<>(content, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);

        return restTemplate.exchange("/api/rooms/" + roomId + "/attachments",
            HttpMethod.POST, new HttpEntity<>(body, headers), MessageResponse.class);
    }
}
