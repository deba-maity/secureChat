CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(48) NOT NULL UNIQUE,
    phone_number VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(96) NOT NULL,
    profile_picture_url TEXT,
    password_hash TEXT NOT NULL,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen TIMESTAMPTZ,
    hide_last_seen BOOLEAN NOT NULL DEFAULT FALSE,
    hide_online_status BOOLEAN NOT NULL DEFAULT FALSE,
    read_receipts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    screenshot_warning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    lock_favorite_chats BOOLEAN NOT NULL DEFAULT FALSE,
    dark_mode BOOLEAN NOT NULL DEFAULT TRUE,
    auto_delete_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    favorite_pin_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_users_username_lookup ON app_users (lower(username));
CREATE INDEX idx_app_users_phone_lookup ON app_users (phone_number);

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_one_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    participant_two_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_conversations_pair UNIQUE (participant_one_id, participant_two_id),
    CONSTRAINT chk_distinct_participants CHECK (participant_one_id <> participant_two_id)
);

CREATE INDEX idx_conversations_participant_one ON conversations (participant_one_id);
CREATE INDEX idx_conversations_participant_two ON conversations (participant_two_id);

CREATE TABLE favorite_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_favorite_user_conversation UNIQUE (user_id, conversation_id)
);

CREATE INDEX idx_favorites_user ON favorite_conversations (user_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    message_type VARCHAR(24) NOT NULL DEFAULT 'TEXT',
    cipher_text TEXT NOT NULL,
    iv VARCHAR(64) NOT NULL,
    temporary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    seen_at TIMESTAMPTZ,
    self_destruct_at TIMESTAMPTZ
);

CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at);
CREATE INDEX idx_messages_recipient_unread ON messages (recipient_id, seen_at);
CREATE INDEX idx_messages_self_destruct ON messages (self_destruct_at);

