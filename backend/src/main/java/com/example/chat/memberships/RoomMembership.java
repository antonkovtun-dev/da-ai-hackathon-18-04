package com.example.chat.memberships;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_memberships")
public class RoomMembership implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RoomRole role;
    @Column(name = "joined_at", nullable = false, updatable = false) private OffsetDateTime joinedAt;

    @PrePersist void onCreate() { joinedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public RoomRole getRole() { return role; }
    public void setRole(RoomRole v) { this.role = v; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
}
