ALTER TABLE attachments
    DROP CONSTRAINT attachments_uploader_id_fkey,
    ADD CONSTRAINT attachments_uploader_id_fkey
        FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE;
