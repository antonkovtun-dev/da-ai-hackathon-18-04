package com.example.chat.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomBanRepository extends JpaRepository<RoomBan, UUID> {
    Optional<RoomBan> findByRoomIdAndUserId(UUID roomId, UUID userId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    List<RoomBan> findByRoomId(UUID roomId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RoomBan b WHERE b.userId = :userId OR b.bannedBy = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
