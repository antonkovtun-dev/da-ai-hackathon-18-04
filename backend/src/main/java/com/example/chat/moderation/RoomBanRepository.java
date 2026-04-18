package com.example.chat.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomBanRepository extends JpaRepository<RoomBan, UUID> {
    Optional<RoomBan> findByRoomIdAndUserId(UUID roomId, UUID userId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    List<RoomBan> findByRoomId(UUID roomId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
