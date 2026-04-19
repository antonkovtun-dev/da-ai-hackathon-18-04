package com.example.chat.rooms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Page<Room> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Room> findByOwnerId(UUID ownerId);
    void deleteByOwnerId(UUID ownerId);
}
