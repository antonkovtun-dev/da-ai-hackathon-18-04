package com.example.chat.moderation;

import com.example.chat.memberships.RoomMembership;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.memberships.RoomRole;
import com.example.chat.moderation.dto.BanResponse;
import com.example.chat.users.User;
import com.example.chat.users.UserRepository;
import com.example.chat.websocket.RoomEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ModerationService {

    private final RoomMembershipRepository membershipRepository;
    private final RoomBanRepository banRepository;
    private final UserRepository userRepository;
    private final RoomEventPublisher eventPublisher;

    public ModerationService(RoomMembershipRepository membershipRepository, RoomBanRepository banRepository,
                             UserRepository userRepository, RoomEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.banRepository = banRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void banUser(UUID roomId, UUID targetUserId, UUID moderatorId, String reason) {
        RoomMembership modM = findMembershipOrThrow(roomId, moderatorId);
        if (modM.getRole() == RoomRole.MEMBER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members cannot ban");

        RoomMembership targetM = membershipRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target not in room"));

        if (modM.getRole() == RoomRole.ADMIN && targetM.getRole() != RoomRole.MEMBER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin cannot ban owner or other admins");

        RoomBan ban = new RoomBan();
        ban.setRoomId(roomId);
        ban.setUserId(targetUserId);
        ban.setBannedBy(moderatorId);
        ban.setReason(reason);
        banRepository.save(ban);

        membershipRepository.deleteByRoomIdAndUserId(roomId, targetUserId);

        User target = userRepository.findById(targetUserId).orElseThrow();
        eventPublisher.publishMemberKicked(roomId, targetUserId, target.getUsername());
    }

    @Transactional
    public void unbanUser(UUID roomId, UUID targetUserId, UUID moderatorId) {
        RoomMembership modM = findMembershipOrThrow(roomId, moderatorId);
        if (modM.getRole() == RoomRole.MEMBER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        banRepository.deleteByRoomIdAndUserId(roomId, targetUserId);
    }

    public List<BanResponse> listBans(UUID roomId, UUID requesterId) {
        RoomMembership m = findMembershipOrThrow(roomId, requesterId);
        if (m.getRole() == RoomRole.MEMBER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        List<RoomBan> bans = banRepository.findByRoomId(roomId);
        Set<UUID> userIds = bans.stream().map(RoomBan::getUserId).collect(Collectors.toSet());
        Map<UUID, String> usernames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        return bans.stream().map(b -> new BanResponse(b.getId(), b.getUserId(),
                usernames.getOrDefault(b.getUserId(), "unknown"),
                b.getBannedBy(), b.getReason(), b.getBannedAt())).toList();
    }

    private RoomMembership findMembershipOrThrow(UUID roomId, UUID userId) {
        return membershipRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
    }
}
