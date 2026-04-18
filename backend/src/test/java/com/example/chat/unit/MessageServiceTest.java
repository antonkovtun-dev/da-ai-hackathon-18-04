package com.example.chat.unit;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.messages.Message;
import com.example.chat.messages.MessageRepository;
import com.example.chat.messages.MessageService;
import com.example.chat.messages.dto.MessageResponse;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock RoomMembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock RoomEventPublisher eventPublisher;
    @InjectMocks MessageService messageService;

    @Test
    void sendMessage_fails_if_not_member() {
        var roomId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        when(membershipRepository.findByRoomIdAndUserId(roomId, userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.sendMessage(roomId, userId, "hello"));
    }

    @Test
    void sendMessage_publishes_event() {
        var roomId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var m = new RoomMembership(); m.setRole(RoomRole.MEMBER);
        when(membershipRepository.findByRoomIdAndUserId(roomId, authorId)).thenReturn(Optional.of(m));
        var user = new User(); user.setUsername("alice"); user.setEmail("a@b.com");
        when(userRepository.findById(authorId)).thenReturn(Optional.of(user));
        var saved = new Message(); saved.setRoomId(roomId); saved.setAuthorId(authorId); saved.setContent("hello");
        when(messageRepository.save(any())).thenReturn(saved);

        messageService.sendMessage(roomId, authorId, "hello");

        verify(eventPublisher).publishMessageNew(eq(roomId), any(MessageResponse.class));
    }

    @Test
    void editMessage_fails_if_not_author() {
        var msgId = UUID.randomUUID();
        var otherId = UUID.randomUUID();
        var msg = new Message(); msg.setAuthorId(UUID.randomUUID());
        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        assertThrows(ResponseStatusException.class,
                () -> messageService.editMessage(msgId, otherId, "new content"));
    }

    @Test
    void deleteMessage_soft_deletes() {
        var msgId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var roomId = UUID.randomUUID();
        var msg = new Message(); msg.setAuthorId(authorId); msg.setRoomId(roomId); msg.setContent("hi");
        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        messageService.deleteMessage(msgId, authorId);

        verify(messageRepository).save(argThat(m -> m.getContent() == null && m.getDeletedAt() != null));
    }
}
