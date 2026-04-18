package com.example.chat.messages;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.createdAt < :before ORDER BY m.createdAt DESC")
    List<Message> findByRoomIdBefore(@Param("roomId") UUID roomId, @Param("before") OffsetDateTime before, Pageable pageable);

    long countByRoomIdAndDeletedAtIsNull(UUID roomId);

    long countByRoomIdAndCreatedAtAfterAndDeletedAtIsNull(UUID roomId, OffsetDateTime since);
}
