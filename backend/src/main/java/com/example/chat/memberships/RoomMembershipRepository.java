package com.example.chat.memberships;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMembershipRepository extends JpaRepository<RoomMembership, UUID> {
    Optional<RoomMembership> findByRoomIdAndUserId(UUID roomId, UUID userId);
    List<RoomMembership> findByRoomId(UUID roomId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByRoomId(UUID roomId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
