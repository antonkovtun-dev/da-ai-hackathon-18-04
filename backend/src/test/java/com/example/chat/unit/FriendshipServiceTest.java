package com.example.chat.unit;

import com.example.chat.bans.UserBlockRepository;
import com.example.chat.friendships.*;
import com.example.chat.friendships.dto.FriendResponse;
import com.example.chat.friendships.dto.SendFriendRequestRequest;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock FriendRequestRepository friendRequestRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock UserRepository userRepository;
    @Mock UserBlockRepository userBlockRepository;

    @InjectMocks FriendshipService friendshipService;

    @Test
    void sendRequest_fails_when_target_not_found() {
        UUID senderId = UUID.randomUUID();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            friendshipService.sendRequest(senderId, new SendFriendRequestRequest("ghost", null)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    @Test
    void sendRequest_fails_when_blocked() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User target = makeUser(receiverId, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(target));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId)).thenReturn(true);

        assertThatThrownBy(() ->
            friendshipService.sendRequest(senderId, new SendFriendRequestRequest("alice", null)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test
    void sendRequest_succeeds_and_saves_request() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User target = makeUser(receiverId, "bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(any(), any())).thenReturn(false);
        when(friendRequestRepository.countPendingBetween(senderId, receiverId, FriendRequestStatus.PENDING)).thenReturn(0L);
        UUID lower = senderId.compareTo(receiverId) < 0 ? senderId : receiverId;
        UUID higher = senderId.compareTo(receiverId) < 0 ? receiverId : senderId;
        when(friendshipRepository.existsByUser1IdAndUser2Id(lower, higher)).thenReturn(false);

        FriendRequest saved = new FriendRequest();
        saved.setSenderId(senderId);
        saved.setReceiverId(receiverId);
        User sender = makeUser(senderId, "me");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(friendRequestRepository.save(any())).thenReturn(saved);

        friendshipService.sendRequest(senderId, new SendFriendRequestRequest("bob", "hey"));

        verify(friendRequestRepository).save(argThat(r ->
            r.getSenderId().equals(senderId) && r.getReceiverId().equals(receiverId)));
    }

    @Test
    void acceptRequest_creates_friendship() {
        UUID requestId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID accepterId = UUID.randomUUID();
        FriendRequest req = new FriendRequest();
        req.setSenderId(senderId);
        req.setReceiverId(accepterId);
        req.setStatus(FriendRequestStatus.PENDING);
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

        friendshipService.acceptRequest(requestId, accepterId);

        assertThat(req.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(friendshipRepository).save(argThat(f ->
            (f.getUser1Id().equals(senderId) || f.getUser1Id().equals(accepterId)) &&
            (f.getUser2Id().equals(senderId) || f.getUser2Id().equals(accepterId))));
    }

    @Test
    void acceptRequest_fails_when_not_receiver() {
        UUID requestId = UUID.randomUUID();
        FriendRequest req = new FriendRequest();
        req.setSenderId(UUID.randomUUID());
        req.setReceiverId(UUID.randomUUID());
        req.setStatus(FriendRequestStatus.PENDING);
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() ->
            friendshipService.acceptRequest(requestId, UUID.randomUUID()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test
    void removeFriend_deletes_friendship() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID lower = userId.compareTo(friendId) < 0 ? userId : friendId;
        UUID higher = userId.compareTo(friendId) < 0 ? friendId : userId;
        when(friendshipRepository.existsByUser1IdAndUser2Id(lower, higher)).thenReturn(true);

        friendshipService.removeFriend(userId, friendId);

        verify(friendshipRepository).deleteBetween(userId, friendId);
    }

    private User makeUser(UUID id, String username) {
        User u = new User();
        u.setUsername(username);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }
}
