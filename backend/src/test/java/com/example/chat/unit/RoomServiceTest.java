package com.example.chat.unit;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.rooms.Room;
import com.example.chat.rooms.RoomRepository;
import com.example.chat.rooms.RoomService;
import com.example.chat.rooms.dto.CreateRoomRequest;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock RoomMembershipRepository membershipRepository;
    @Mock RoomBanRepository banRepository;
    @Mock UserRepository userRepository;
    @Mock RoomEventPublisher eventPublisher;
    @InjectMocks RoomService roomService;

    @Test
    void createRoom_saves_room_and_owner_membership() {
        var ownerId = UUID.randomUUID();
        var req = new CreateRoomRequest("general", "A room");
        var savedRoom = new Room();
        savedRoom.setName("general");
        savedRoom.setOwnerId(ownerId);
        when(roomRepository.save(any())).thenReturn(savedRoom);
        when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var user = new User(); user.setUsername("alice"); user.setEmail("a@b.com");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user));

        roomService.createRoom(req, ownerId);

        var roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getOwnerId()).isEqualTo(ownerId);
        assertThat(roomCaptor.getValue().getName()).isEqualTo("general");

        var memberCaptor = ArgumentCaptor.forClass(RoomMembership.class);
        verify(membershipRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(RoomRole.OWNER);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(ownerId);
    }

    @Test
    void joinRoom_fails_if_banned() {
        var roomId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(new Room()));
        when(banRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(roomId, userId));
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void joinRoom_fails_if_already_member() {
        var roomId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(new Room()));
        when(banRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(false);
        when(membershipRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> roomService.joinRoom(roomId, userId));
        assertThat(ex.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void leaveRoom_fails_if_owner() {
        var roomId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var membership = new RoomMembership();
        membership.setRole(RoomRole.OWNER);
        membership.setUserId(ownerId);
        when(membershipRepository.findByRoomIdAndUserId(roomId, ownerId))
                .thenReturn(Optional.of(membership));

        assertThrows(ResponseStatusException.class,
                () -> roomService.leaveRoom(roomId, ownerId));
    }

    @Test
    void deleteRoom_fails_if_not_owner() {
        var roomId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var membership = new RoomMembership();
        membership.setRole(RoomRole.MEMBER);
        when(membershipRepository.findByRoomIdAndUserId(roomId, userId))
                .thenReturn(Optional.of(membership));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(new Room()));

        assertThrows(ResponseStatusException.class,
                () -> roomService.deleteRoom(roomId, userId));
    }
}
