package com.example.chat.unit;

import com.example.chat.bans.UserBlock;
import com.example.chat.bans.UserBlockRepository;
import com.example.chat.bans.UserBlockService;
import com.example.chat.bans.dto.BlockRequest;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendRequestStatus;
import com.example.chat.friendships.FriendshipRepository;
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
class UserBlockServiceTest {

    @Mock UserBlockRepository userBlockRepository;
    @Mock UserRepository userRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock FriendRequestRepository friendRequestRepository;

    @InjectMocks UserBlockService userBlockService;

    @Test
    void blockUser_removes_friendship_and_cancels_requests() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User target = new User(); target.setUsername("target");
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(target));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);

        userBlockService.blockUser(blockerId, new BlockRequest(blockedId));

        verify(userBlockRepository).save(argThat(b ->
            b.getBlockerId().equals(blockerId) && b.getBlockedId().equals(blockedId)));
        verify(friendshipRepository).deleteBetween(blockerId, blockedId);
        verify(friendRequestRepository).cancelPendingBetween(blockerId, blockedId,
            FriendRequestStatus.PENDING, FriendRequestStatus.CANCELED);
    }

    @Test
    void blockUser_fails_when_already_blocked() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User target = new User(); target.setUsername("target");
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(target));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(true);

        assertThatThrownBy(() -> userBlockService.blockUser(blockerId, new BlockRequest(blockedId)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    @Test
    void isBlocked_checks_both_directions() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        when(userBlockRepository.existsByBlockerIdAndBlockedId(a, b)).thenReturn(false);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(b, a)).thenReturn(true);

        assertThat(userBlockService.isBlocked(a, b)).isTrue();
    }
}
