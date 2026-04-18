package com.example.chat.bans;

import com.example.chat.bans.dto.BlockRequest;
import com.example.chat.bans.dto.BlockedUserResponse;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendRequestStatus;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;

    public UserBlockService(UserBlockRepository userBlockRepository,
                            UserRepository userRepository,
                            FriendshipRepository friendshipRepository,
                            FriendRequestRepository friendRequestRepository) {
        this.userBlockRepository = userBlockRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    @Transactional
    public void blockUser(UUID blockerId, BlockRequest req) {
        UUID blockedId = req.targetUserId();
        if (blockerId.equals(blockedId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        userRepository.findById(blockedId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already blocked");

        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        userBlockRepository.save(block);

        friendshipRepository.deleteBetween(blockerId, blockedId);
        friendRequestRepository.cancelPendingBetween(blockerId, blockedId,
            FriendRequestStatus.PENDING, FriendRequestStatus.CANCELED);
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not blocked");
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> listBlocked(UUID blockerId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerId(blockerId);
        Map<UUID, User> users = userRepository.findAllById(
            blocks.stream().map(UserBlock::getBlockedId).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        return blocks.stream()
            .map(b -> {
                User u = users.get(b.getBlockedId());
                return new BlockedUserResponse(b.getBlockedId(),
                    u != null ? u.getUsername() : "unknown", b.getCreatedAt());
            }).toList();
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(UUID a, UUID b) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(a, b) ||
               userBlockRepository.existsByBlockerIdAndBlockedId(b, a);
    }
}
