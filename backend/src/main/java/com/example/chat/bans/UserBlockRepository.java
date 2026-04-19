package com.example.chat.bans;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    @Transactional
    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    List<UserBlock> findByBlockerId(UUID blockerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserBlock b WHERE b.blockerId = :userId OR b.blockedId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
