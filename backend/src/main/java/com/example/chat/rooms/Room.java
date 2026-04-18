package com.example.chat.rooms;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID v) { this.ownerId = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
