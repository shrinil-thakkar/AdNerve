CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE screens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    location     VARCHAR(255),
    device_token VARCHAR(512) UNIQUE NOT NULL,
    timezone     VARCHAR(64) DEFAULT 'UTC',
    is_active    BOOLEAN DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE slots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id        UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    slot_type        VARCHAR(20) NOT NULL CHECK (slot_type IN ('time_based', 'sequential')),
    start_time       TIME,
    end_time         TIME,
    days_of_week     INTEGER[],
    sequence_order   INTEGER,
    duration_seconds INTEGER,
    is_active        BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMPTZ DEFAULT now(),
    updated_at       TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE media (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    media_type       VARCHAR(20) NOT NULL CHECK (media_type IN ('image', 'video')),
    s3_key           VARCHAR(1024) NOT NULL UNIQUE,
    s3_bucket        VARCHAR(255) NOT NULL,
    file_size_bytes  BIGINT,
    duration_seconds INTEGER,
    mime_type        VARCHAR(100),
    thumbnail_s3_key VARCHAR(1024),
    uploaded_by      UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE slot_assignments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id     UUID NOT NULL REFERENCES slots(id) ON DELETE CASCADE,
    media_id    UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    priority    INTEGER DEFAULT 0,
    is_active   BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(slot_id, media_id)
);

CREATE TABLE impression_logs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id               UUID NOT NULL REFERENCES screens(id),
    slot_id                 UUID NOT NULL REFERENCES slots(id),
    media_id                UUID NOT NULL REFERENCES media(id),
    played_at               TIMESTAMPTZ NOT NULL,
    duration_played_seconds INTEGER,
    completed               BOOLEAN DEFAULT FALSE
);
