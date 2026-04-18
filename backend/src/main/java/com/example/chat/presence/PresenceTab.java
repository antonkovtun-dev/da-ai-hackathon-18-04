package com.example.chat.presence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "presence_tabs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tab_id"}))
public class PresenceTab {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tab_id", nullable = false, length = 64)
    private String tabId;

    @Column(name = "last_heartbeat_at", nullable = false)
    private OffsetDateTime lastHeartbeatAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getTabId() { return tabId; }
    public void setTabId(String v) { this.tabId = v; }
    public OffsetDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(OffsetDateTime v) { this.lastHeartbeatAt = v; }
    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(OffsetDateTime v) { this.lastActivityAt = v; }
}
