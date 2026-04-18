package com.example.chat.memberships;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMembershipRepository extends JpaRepository<RoomMembership, UUID> {
    Optional<RoomMembership> findByRoomIdAndUserId(UUID roomId, UUID userId);
    List<RoomMembership> findByRoomId(UUID roomId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByRoomId(UUID roomId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    @Query("SELECT m.roomId FROM RoomMembership m WHERE m.userId = :userId")
    List<UUID> findRoomIdsByUserId(UUID userId);
}
