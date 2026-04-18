package com.example.chat.unit;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.moderation.ModerationService;
import com.example.chat.moderation.RoomBan;
import com.example.chat.moderation.RoomBanRepository;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock RoomMembershipRepository membershipRepository;
    @Mock RoomBanRepository banRepository;
    @Mock UserRepository userRepository;
    @Mock RoomEventPublisher eventPublisher;
    @InjectMocks ModerationService moderationService;

    @Test
    void member_cannot_ban() {
        var roomId = UUID.randomUUID();
        var modId = UUID.randomUUID();
        var membership = new RoomMembership(); membership.setRole(RoomRole.MEMBER);
        when(membershipRepository.findByRoomIdAndUserId(roomId, modId)).thenReturn(Optional.of(membership));

        assertThrows(ResponseStatusException.class,
                () -> moderationService.banUser(roomId, UUID.randomUUID(), modId, null));
    }

    @Test
    void admin_cannot_ban_owner() {
        var roomId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var adminM = new RoomMembership(); adminM.setRole(RoomRole.ADMIN);
        var targetM = new RoomMembership(); targetM.setRole(RoomRole.OWNER);
        when(membershipRepository.findByRoomIdAndUserId(roomId, adminId)).thenReturn(Optional.of(adminM));
        when(membershipRepository.findByRoomIdAndUserId(roomId, targetId)).thenReturn(Optional.of(targetM));

        assertThrows(ResponseStatusException.class,
                () -> moderationService.banUser(roomId, targetId, adminId, null));
    }

    @Test
    void owner_can_ban_admin() {
        var roomId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var ownerM = new RoomMembership(); ownerM.setRole(RoomRole.OWNER);
        var targetM = new RoomMembership(); targetM.setRole(RoomRole.ADMIN);
        when(membershipRepository.findByRoomIdAndUserId(roomId, ownerId)).thenReturn(Optional.of(ownerM));
        when(membershipRepository.findByRoomIdAndUserId(roomId, targetId)).thenReturn(Optional.of(targetM));
        when(banRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var user = new User(); user.setUsername("alice"); user.setEmail("a@b.com");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));

        moderationService.banUser(roomId, targetId, ownerId, "spam");

        verify(banRepository).save(any(RoomBan.class));
        verify(membershipRepository).deleteByRoomIdAndUserId(roomId, targetId);
    }
}
