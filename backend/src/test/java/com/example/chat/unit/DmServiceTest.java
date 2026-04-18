package com.example.chat.unit;

import com.example.chat.bans.UserBlockRepository;
import com.example.chat.dm.*;
import com.example.chat.dm.dto.DmMessageResponse;
import com.example.chat.dm.dto.DmThreadResponse;
import com.example.chat.dm.dto.SendDmRequest;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DmServiceTest {

    @Mock DmThreadRepository dmThreadRepository;
    @Mock DmMessageRepository dmMessageRepository;
    @Mock UserRepository userRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock UserBlockRepository userBlockRepository;
    @Mock DmEventPublisher dmEventPublisher;

    @InjectMocks DmService dmService;

    @Test
    void sendMessage_fails_when_not_friends() {
        UUID requesterId = UUID.randomUUID(), otherId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        DmThread thread = makeThread(threadId, requesterId, otherId);
        when(dmThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        UUID lower = requesterId.compareTo(otherId) < 0 ? requesterId : otherId;
        UUID higher = requesterId.compareTo(otherId) < 0 ? otherId : requesterId;
        when(friendshipRepository.existsByUser1IdAndUser2Id(lower, higher)).thenReturn(false);

        assertThatThrownBy(() ->
            dmService.sendMessage(requesterId, threadId, new SendDmRequest("hi")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test
    void sendMessage_fails_when_blocked() {
        UUID requesterId = UUID.randomUUID(), otherId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        DmThread thread = makeThread(threadId, requesterId, otherId);
        when(dmThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        UUID lower = requesterId.compareTo(otherId) < 0 ? requesterId : otherId;
        UUID higher = requesterId.compareTo(otherId) < 0 ? otherId : requesterId;
        when(friendshipRepository.existsByUser1IdAndUser2Id(lower, higher)).thenReturn(true);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(requesterId, otherId)).thenReturn(true);

        assertThatThrownBy(() ->
            dmService.sendMessage(requesterId, threadId, new SendDmRequest("hi")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test
    void sendMessage_succeeds_and_publishes_event() {
        UUID requesterId = UUID.randomUUID(), otherId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        DmThread thread = makeThread(threadId, requesterId, otherId);
        when(dmThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        UUID lower = requesterId.compareTo(otherId) < 0 ? requesterId : otherId;
        UUID higher = requesterId.compareTo(otherId) < 0 ? otherId : requesterId;
        when(friendshipRepository.existsByUser1IdAndUser2Id(lower, higher)).thenReturn(true);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(any(), any())).thenReturn(false);
        User author = new User(); author.setUsername("alice");
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(author));

        DmMessage saved = new DmMessage();
        saved.setThreadId(threadId);
        saved.setAuthorId(requesterId);
        saved.setContent("hi");
        try {
            var f = DmMessage.class.getDeclaredField("id"); f.setAccessible(true); f.set(saved, UUID.randomUUID());
            var c = DmMessage.class.getDeclaredField("createdAt"); c.setAccessible(true); c.set(saved, OffsetDateTime.now());
        } catch (Exception e) { throw new RuntimeException(e); }
        when(dmMessageRepository.save(any())).thenReturn(saved);

        dmService.sendMessage(requesterId, threadId, new SendDmRequest("hi"));

        verify(dmMessageRepository).save(any());
        verify(dmEventPublisher).publishMessageNew(eq(threadId), any());
    }

    private DmThread makeThread(UUID id, UUID user1, UUID user2) {
        UUID u1 = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID u2 = user1.compareTo(user2) < 0 ? user2 : user1;
        DmThread t = new DmThread();
        t.setUser1Id(u1);
        t.setUser2Id(u2);
        try {
            var f = DmThread.class.getDeclaredField("id"); f.setAccessible(true); f.set(t, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return t;
    }
}
