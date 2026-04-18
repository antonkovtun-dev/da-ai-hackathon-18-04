package com.example.chat.readstate;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "read_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class ReadState {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_at", nullable = false)
    private OffsetDateTime lastReadAt;

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public OffsetDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(OffsetDateTime v) { this.lastReadAt = v; }
}
