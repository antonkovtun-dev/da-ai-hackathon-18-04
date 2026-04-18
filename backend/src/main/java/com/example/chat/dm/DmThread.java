package com.example.chat.dm;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dm_threads")
public class DmThread implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getUser1Id() { return user1Id; }
    public void setUser1Id(UUID user1Id) { this.user1Id = user1Id; }
    public UUID getUser2Id() { return user2Id; }
    public void setUser2Id(UUID user2Id) { this.user2Id = user2Id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
