ALTER TABLE attachments ADD CONSTRAINT uq_attachments_message_id UNIQUE (message_id);
