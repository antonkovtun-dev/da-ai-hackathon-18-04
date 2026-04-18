package com.example.chat.dm;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DmMessageRepository extends JpaRepository<DmMessage, UUID> {

    List<DmMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    @Query("SELECT m FROM DmMessage m WHERE m.threadId = :threadId AND m.createdAt < :before " +
           "ORDER BY m.createdAt DESC")
    List<DmMessage> findByThreadIdBefore(@Param("threadId") UUID threadId,
                                         @Param("before") OffsetDateTime before,
                                         Pageable pageable);
}
