package com.example.chat.readstate;

import com.example.chat.memberships.RoomMembershipRepository;
import com.example.chat.messages.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ReadStateService {

    private final ReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;
    private final RoomMembershipRepository membershipRepository;

    public ReadStateService(ReadStateRepository readStateRepository,
                            MessageRepository messageRepository,
                            RoomMembershipRepository membershipRepository) {
        this.readStateRepository = readStateRepository;
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void markRead(UUID roomId, UUID userId) {
        ReadState rs = readStateRepository.findByRoomIdAndUserId(roomId, userId)
            .orElseGet(() -> {
                ReadState s = new ReadState();
                s.setRoomId(roomId);
                s.setUserId(userId);
                return s;
            });
        rs.setLastReadAt(OffsetDateTime.now());
        readStateRepository.save(rs);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> getUnreadCounts(UUID userId) {
        List<UUID> roomIds = membershipRepository.findRoomIdsByUserId(userId);
        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (UUID roomId : roomIds) {
            Optional<ReadState> rs = readStateRepository.findByRoomIdAndUserId(roomId, userId);
            long count;
            if (rs.isEmpty()) {
                count = messageRepository.countByRoomIdAndDeletedAtIsNull(roomId);
            } else {
                count = messageRepository.countByRoomIdAndCreatedAtAfterAndDeletedAtIsNull(
                    roomId, rs.get().getLastReadAt());
            }
            counts.put(roomId, count);
        }
        return counts;
    }
}
