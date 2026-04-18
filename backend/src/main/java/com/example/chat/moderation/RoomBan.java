package com.example.chat.moderation;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_bans")
public class RoomBan implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "banned_by", nullable = false) private UUID bannedBy;
    @Column(columnDefinition = "text") private String reason;
    @Column(name = "banned_at", nullable = false, updatable = false) private OffsetDateTime bannedAt;

    @PrePersist void onCreate() { bannedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public UUID getBannedBy() { return bannedBy; }
    public void setBannedBy(UUID v) { this.bannedBy = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public OffsetDateTime getBannedAt() { return bannedAt; }
}
