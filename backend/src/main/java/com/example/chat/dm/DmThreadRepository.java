package com.example.chat.dm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmThreadRepository extends JpaRepository<DmThread, UUID> {
    Optional<DmThread> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
    List<DmThread> findByUser1IdOrUser2Id(UUID user1Id, UUID user2Id);

    @Modifying
    @Transactional
    @Query("DELETE FROM DmThread t WHERE t.user1Id = :userId OR t.user2Id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
