package com.example.chat.readstate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReadStateRepository extends JpaRepository<ReadState, UUID> {
    Optional<ReadState> findByRoomIdAndUserId(UUID roomId, UUID userId);
}
