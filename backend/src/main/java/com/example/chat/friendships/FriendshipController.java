package com.example.chat.friendships;

import com.example.chat.auth.AuthUserDetails;
import com.example.chat.friendships.dto.FriendRequestResponse;
import com.example.chat.friendships.dto.FriendResponse;
import com.example.chat.friendships.dto.SendFriendRequestRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestResponse sendRequest(@Valid @RequestBody SendFriendRequestRequest req) {
        return friendshipService.sendRequest(currentUserId(), req);
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestResponse> incoming() {
        return friendshipService.listIncoming(currentUserId());
    }

    @GetMapping("/requests/outgoing")
    public List<FriendRequestResponse> outgoing() {
        return friendshipService.listOutgoing(currentUserId());
    }

    @PostMapping("/requests/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable UUID id) {
        friendshipService.acceptRequest(id, currentUserId());
    }

    @PostMapping("/requests/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable UUID id) {
        friendshipService.declineRequest(id, currentUserId());
    }

    @DeleteMapping("/requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        friendshipService.cancelRequest(id, currentUserId());
    }

    @GetMapping
    public List<FriendResponse> listFriends() {
        return friendshipService.listFriends(currentUserId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable UUID userId) {
        friendshipService.removeFriend(currentUserId(), userId);
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
    }
}
