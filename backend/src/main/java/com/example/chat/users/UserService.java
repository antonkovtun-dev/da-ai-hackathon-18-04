package com.example.chat.users;

import com.example.chat.bans.UserBlockRepository;
import com.example.chat.common.FieldException;
import com.example.chat.dm.DmThreadRepository;
import com.example.chat.friendships.FriendRequestRepository;
import com.example.chat.friendships.FriendshipRepository;
import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.moderation.RoomBanRepository;
import com.example.chat.presence.PresenceTabRepository;
import com.example.chat.readstate.ReadStateRepository;
import com.example.chat.rooms.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomMembershipRepository membershipRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserBlockRepository userBlockRepository;
    private final RoomBanRepository roomBanRepository;
    private final DmThreadRepository dmThreadRepository;
    private final PresenceTabRepository presenceTabRepository;
    private final ReadStateRepository readStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UserService(UserRepository userRepository,
                       RoomRepository roomRepository,
                       RoomMembershipRepository membershipRepository,
                       FriendshipRepository friendshipRepository,
                       FriendRequestRepository friendRequestRepository,
                       UserBlockRepository userBlockRepository,
                       RoomBanRepository roomBanRepository,
                       DmThreadRepository dmThreadRepository,
                       PresenceTabRepository presenceTabRepository,
                       ReadStateRepository readStateRepository,
                       PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.membershipRepository = membershipRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userBlockRepository = userBlockRepository;
        this.roomBanRepository = roomBanRepository;
        this.dmThreadRepository = dmThreadRepository;
        this.presenceTabRepository = presenceTabRepository;
        this.readStateRepository = readStateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        roomRepository.deleteByOwnerId(userId);
        membershipRepository.deleteByUserId(userId);
        roomBanRepository.deleteByUserId(userId);
        friendshipRepository.deleteByUserId(userId);
        friendRequestRepository.deleteByUserId(userId);
        userBlockRepository.deleteByUserId(userId);
        dmThreadRepository.deleteByUserId(userId);
        presenceTabRepository.deleteByUserId(userId);
        readStateRepository.deleteByUserId(userId);

        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        jdbcTemplate.update("DELETE FROM spring_session WHERE principal_name = ?", user.getEmail());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new FieldException("currentPassword", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
