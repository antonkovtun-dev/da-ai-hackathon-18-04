package com.example.chat.messages;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "author_id", nullable = false) private UUID authorId;
    @Column(columnDefinition = "text") private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "edited_at") private OffsetDateTime editedAt;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID v) { this.roomId = v; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID v) { this.authorId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(OffsetDateTime v) { this.editedAt = v; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime v) { this.deletedAt = v; }
}
