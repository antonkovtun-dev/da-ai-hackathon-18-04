-- read_states: tracks when each user last read each room
CREATE TABLE read_states (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, user_id)
);

CREATE INDEX idx_read_states_user ON read_states(user_id);

-- presence_tabs: one row per browser tab per user
CREATE TABLE presence_tabs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tab_id            VARCHAR(64) NOT NULL,
    last_heartbeat_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, tab_id)
);

CREATE INDEX idx_presence_tabs_user ON presence_tabs(user_id);
