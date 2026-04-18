package com.example.chat.dm;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmThreadRepository extends JpaRepository<DmThread, UUID> {
    Optional<DmThread> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
    List<DmThread> findByUser1IdOrUser2Id(UUID user1Id, UUID user2Id);
}
