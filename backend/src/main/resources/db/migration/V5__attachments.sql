CREATE TABLE attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    room_id      UUID NOT NULL REFERENCES rooms(id)    ON DELETE CASCADE,
    uploader_id  UUID NOT NULL REFERENCES users(id),
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size         BIGINT NOT NULL,
    stored_path  VARCHAR(512) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_message ON attachments(message_id);
CREATE INDEX idx_attachments_room    ON attachments(room_id);
