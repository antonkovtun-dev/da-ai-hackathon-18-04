package com.example.chat.attachments;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "message_id",   nullable = false) private UUID messageId;
    @Column(name = "room_id",      nullable = false) private UUID roomId;
    @Column(name = "uploader_id",  nullable = false) private UUID uploaderId;
    @Column(name = "filename",     nullable = false) private String filename;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size",         nullable = false) private long size;
    @Column(name = "stored_path",  nullable = false) private String storedPath;
    @Column(name = "created_at",   nullable = false, updatable = false) private OffsetDateTime createdAt;

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); }

    public UUID getId()            { return id; }
    public UUID getMessageId()     { return messageId; }
    public void setMessageId(UUID v) { this.messageId = v; }
    public UUID getRoomId()        { return roomId; }
    public void setRoomId(UUID v)  { this.roomId = v; }
    public UUID getUploaderId()    { return uploaderId; }
    public void setUploaderId(UUID v) { this.uploaderId = v; }
    public String getFilename()    { return filename; }
    public void setFilename(String v) { this.filename = v; }
    public String getContentType() { return contentType; }
    public void setContentType(String v) { this.contentType = v; }
    public long getSize()          { return size; }
    public void setSize(long v)    { this.size = v; }
    public String getStoredPath()  { return storedPath; }
    public void setStoredPath(String v) { this.storedPath = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
