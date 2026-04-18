package com.example.chat.dm;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dm_messages")
public class DmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public UUID getThreadId() { return threadId; }
    public void setThreadId(UUID threadId) { this.threadId = threadId; }
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(OffsetDateTime editedAt) { this.editedAt = editedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
