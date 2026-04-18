package com.example.chat.rooms;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.memberships.dto.MemberResponse;
import com.example.chat.memberships.dto.SetRoleRequest;
import com.example.chat.rooms.dto.CreateRoomRequest;
import com.example.chat.rooms.dto.RoomResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) { this.roomService = roomService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest req) {
        return roomService.createRoom(req, currentUserId());
    }

    @GetMapping
    public Page<RoomResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.listRooms(search, page, Math.min(size, 50));
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable UUID id) {
        return roomService.getRoom(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roomService.deleteRoom(id, currentUserId());
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable UUID id) {
        roomService.joinRoom(id, currentUserId());
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id) {
        roomService.leaveRoom(id, currentUserId());
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> members(@PathVariable UUID id) {
        return roomService.getMembers(id, currentUserId());
    }

    @PutMapping("/{id}/members/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setRole(@PathVariable UUID id, @PathVariable UUID userId,
                        @Valid @RequestBody SetRoleRequest req) {
        roomService.setMemberRole(id, userId, req, currentUserId());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
