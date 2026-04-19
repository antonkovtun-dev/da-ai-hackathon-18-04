CREATE TABLE user_session_meta (
    session_id  VARCHAR(36)  PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_agent  VARCHAR(512),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_usm_user_id ON user_session_meta(user_id);
