package com.example.chat.presence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresenceTabRepository extends JpaRepository<PresenceTab, UUID> {

    Optional<PresenceTab> findByUserIdAndTabId(UUID userId, String tabId);

    List<PresenceTab> findByUserId(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PresenceTab t WHERE t.userId = :userId AND t.lastHeartbeatAt < :before")
    void deleteStale(UUID userId, OffsetDateTime before);
}
