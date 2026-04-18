package com.example.chat.friendships;

import com.example.chat.bans.UserBlockRepository;
import com.example.chat.friendships.dto.FriendRequestResponse;
import com.example.chat.friendships.dto.FriendResponse;
import com.example.chat.friendships.dto.SendFriendRequestRequest;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    public FriendshipService(FriendRequestRepository friendRequestRepository,
                             FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             UserBlockRepository userBlockRepository) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userBlockRepository = userBlockRepository;
    }

    @Transactional
    public FriendRequestResponse sendRequest(UUID senderId, SendFriendRequestRequest req) {
        User target = userRepository.findByUsername(req.targetUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UUID receiverId = target.getId();

        if (senderId.equals(receiverId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot friend yourself");
        if (userBlockRepository.existsByBlockerIdAndBlockedId(senderId, receiverId) ||
            userBlockRepository.existsByBlockerIdAndBlockedId(receiverId, senderId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked");
        if (friendRequestRepository.countPendingBetween(senderId, receiverId, FriendRequestStatus.PENDING) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pending request already exists");
        if (friendshipRepository.existsByUser1IdAndUser2Id(lower(senderId, receiverId), higher(senderId, receiverId)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");

        FriendRequest fr = new FriendRequest();
        fr.setSenderId(senderId);
        fr.setReceiverId(receiverId);
        fr.setMessage(req.message());
        fr = friendRequestRepository.save(fr);

        User sender = userRepository.findById(senderId).orElseThrow();
        return toRequestResponse(fr, sender.getUsername(), target.getUsername());
    }

    @Transactional
    public void acceptRequest(UUID requestId, UUID accepterId) {
        FriendRequest req = findRequestOrThrow(requestId);
        if (!req.getReceiverId().equals(accepterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (req.getStatus() != FriendRequestStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not pending");

        req.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(req);

        Friendship f = new Friendship();
        f.setUser1Id(lower(req.getSenderId(), req.getReceiverId()));
        f.setUser2Id(higher(req.getSenderId(), req.getReceiverId()));
        friendshipRepository.save(f);
    }

    @Transactional
    public void declineRequest(UUID requestId, UUID declinerId) {
        FriendRequest req = findRequestOrThrow(requestId);
        if (!req.getReceiverId().equals(declinerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (req.getStatus() != FriendRequestStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not pending");
        req.setStatus(FriendRequestStatus.DECLINED);
        friendRequestRepository.save(req);
    }

    @Transactional
    public void cancelRequest(UUID requestId, UUID cancelerId) {
        FriendRequest req = findRequestOrThrow(requestId);
        if (!req.getSenderId().equals(cancelerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (req.getStatus() != FriendRequestStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not pending");
        req.setStatus(FriendRequestStatus.CANCELED);
        friendRequestRepository.save(req);
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        if (!friendshipRepository.existsByUser1IdAndUser2Id(lower(userId, friendId), higher(userId, friendId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not friends");
        friendshipRepository.deleteBetween(userId, friendId);
    }

    public List<FriendResponse> listFriends(UUID userId) {
        List<Friendship> friendships = friendshipRepository.findByUser1IdOrUser2Id(userId, userId);
        List<UUID> friendIds = friendships.stream()
            .map(f -> f.getUser1Id().equals(userId) ? f.getUser2Id() : f.getUser1Id())
            .toList();
        Map<UUID, User> users = userRepository.findAllById(friendIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        return friendships.stream()
            .map(f -> {
                UUID fid = f.getUser1Id().equals(userId) ? f.getUser2Id() : f.getUser1Id();
                User u = users.get(fid);
                return new FriendResponse(fid, u != null ? u.getUsername() : "unknown", f.getCreatedAt());
            }).toList();
    }

    public List<FriendRequestResponse> listIncoming(UUID userId) {
        List<FriendRequest> reqs = friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
        List<UUID> senderIds = reqs.stream().map(FriendRequest::getSenderId).toList();
        Map<UUID, User> users = userRepository.findAllById(senderIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        User me = userRepository.findById(userId).orElseThrow();
        return reqs.stream()
            .map(r -> toRequestResponse(r,
                users.containsKey(r.getSenderId()) ? users.get(r.getSenderId()).getUsername() : "unknown",
                me.getUsername()))
            .toList();
    }

    public List<FriendRequestResponse> listOutgoing(UUID userId) {
        List<FriendRequest> reqs = friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
        List<UUID> receiverIds = reqs.stream().map(FriendRequest::getReceiverId).toList();
        Map<UUID, User> users = userRepository.findAllById(receiverIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        User me = userRepository.findById(userId).orElseThrow();
        return reqs.stream()
            .map(r -> toRequestResponse(r,
                me.getUsername(),
                users.containsKey(r.getReceiverId()) ? users.get(r.getReceiverId()).getUsername() : "unknown"))
            .toList();
    }

    public boolean areFriends(UUID a, UUID b) {
        return friendshipRepository.existsByUser1IdAndUser2Id(lower(a, b), higher(a, b));
    }

    private FriendRequest findRequestOrThrow(UUID id) {
        return friendRequestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    private FriendRequestResponse toRequestResponse(FriendRequest r, String senderUsername, String receiverUsername) {
        return new FriendRequestResponse(r.getId(), r.getSenderId(), senderUsername,
            r.getReceiverId(), receiverUsername, r.getMessage(), r.getStatus(),
            r.getCreatedAt(), r.getUpdatedAt());
    }

    public static UUID lower(UUID a, UUID b)  { return a.compareTo(b) < 0 ? a : b; }
    public static UUID higher(UUID a, UUID b) { return a.compareTo(b) < 0 ? b : a; }
}
