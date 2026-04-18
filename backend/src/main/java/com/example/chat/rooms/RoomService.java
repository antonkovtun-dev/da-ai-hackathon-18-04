package com.example.chat.rooms;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.memberships.dto.MemberResponse;
import com.example.chat.memberships.dto.SetRoleRequest;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.rooms.dto.CreateRoomRequest;
import com.example.chat.rooms.dto.RoomResponse;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMembershipRepository membershipRepository;
    private final RoomBanRepository banRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;

    public RoomService(RoomRepository roomRepository, RoomMembershipRepository membershipRepository,
                       RoomBanRepository banRepository, UserRepository userRepository,
                       RoomEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.membershipRepository = membershipRepository;
        this.banRepository = banRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req, UUID ownerId) {
        Room room = new Room();
        room.setName(req.name());
        room.setDescription(req.description());
        room.setOwnerId(ownerId);
        room = roomRepository.save(room);

        RoomMembership m = new RoomMembership();
        m.setRoomId(room.getId());
        m.setUserId(ownerId);
        m.setRole(RoomRole.OWNER);
        membershipRepository.save(m);

        User owner = userRepository.findById(ownerId).orElseThrow();
        eventPublisher.publishMemberJoined(room.getId(), ownerId, owner.getUsername());
        return toResponse(room, 1);
    }

    public Page<RoomResponse> listRooms(String search, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Room> rooms = (search == null || search.isBlank())
                ? roomRepository.findAll(pageable)
                : roomRepository.findByNameContainingIgnoreCase(search, pageable);
        return rooms.map(r -> toResponse(r, membershipRepository.countByRoomId(r.getId())));
    }

    public RoomResponse getRoom(UUID roomId) {
        Room room = findRoomOrThrow(roomId);
        return toResponse(room, membershipRepository.countByRoomId(roomId));
    }

    @Transactional
    public void deleteRoom(UUID roomId, UUID requesterId) {
        findRoomOrThrow(roomId);
        RoomMembership m = findMembershipOrThrow(roomId, requesterId);
        if (m.getRole() != RoomRole.OWNER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        roomRepository.deleteById(roomId);
    }

    @Transactional
    public void joinRoom(UUID roomId, UUID userId) {
        findRoomOrThrow(roomId);
        if (banRepository.existsByRoomIdAndUserId(roomId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this room");
        if (membershipRepository.existsByRoomIdAndUserId(roomId, userId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already a member");

        RoomMembership m = new RoomMembership();
        m.setRoomId(roomId);
        m.setUserId(userId);
        m.setRole(RoomRole.MEMBER);
        membershipRepository.save(m);

        User user = userRepository.findById(userId).orElseThrow();
        eventPublisher.publishMemberJoined(roomId, userId, user.getUsername());
    }

    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        RoomMembership m = findMembershipOrThrow(roomId, userId);
        if (m.getRole() == RoomRole.OWNER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner cannot leave");
        membershipRepository.deleteByRoomIdAndUserId(roomId, userId);
        User user = userRepository.findById(userId).orElseThrow();
        eventPublisher.publishMemberLeft(roomId, userId, user.getUsername());
    }

    public List<MemberResponse> getMembers(UUID roomId, UUID requesterId) {
        findMembershipOrThrow(roomId, requesterId);
        List<RoomMembership> memberships = membershipRepository.findByRoomId(roomId);
        Map<UUID, User> users = userRepository.findAllById(
                memberships.stream().map(RoomMembership::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        return memberships.stream()
                .map(m -> new MemberResponse(m.getUserId(),
                        users.getOrDefault(m.getUserId(), new User()) != null
                                ? users.containsKey(m.getUserId()) ? users.get(m.getUserId()).getUsername() : "unknown"
                                : "unknown",
                        m.getRole(), m.getJoinedAt()))
                .toList();
    }

    @Transactional
    public void setMemberRole(UUID roomId, UUID targetUserId, SetRoleRequest req, UUID requesterId) {
        RoomMembership requesterM = findMembershipOrThrow(roomId, requesterId);
        if (requesterM.getRole() != RoomRole.OWNER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (req.role() == RoomRole.OWNER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot promote to OWNER");
        RoomMembership targetM = findMembershipOrThrow(roomId, targetUserId);
        if (targetM.getRole() == RoomRole.OWNER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot demote OWNER");
        targetM.setRole(req.role());
        membershipRepository.save(targetM);
    }

    public RoomMembership findMembershipOrThrow(UUID roomId, UUID userId) {
        return membershipRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
    }

    private Room findRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private RoomResponse toResponse(Room r, long memberCount) {
        return new RoomResponse(r.getId(), r.getName(), r.getDescription(), r.getOwnerId(), memberCount, r.getCreatedAt());
    }
}
