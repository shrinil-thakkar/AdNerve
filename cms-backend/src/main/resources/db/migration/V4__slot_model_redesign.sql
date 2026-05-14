-- V4: Replace slot model with category-based hierarchy.

-- 1. Drop the old slots table (CASCADE removes old slot_assignments rows too).
DROP TABLE IF EXISTS slot_assignments CASCADE;
DROP TABLE IF EXISTS slots CASCADE;

-- 2. Add slot defaults to screens.
ALTER TABLE screens
    ADD COLUMN default_slot_length_seconds INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN default_slot_count INTEGER NOT NULL DEFAULT 6;

-- 3. New table: screen_categories. Exactly 3 rows per screen.
CREATE TABLE screen_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id   UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    category    VARCHAR(20) NOT NULL CHECK (category IN ('MORNING', 'AFTERNOON', 'EVENING')),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(screen_id, category),
    CHECK (start_time < end_time)
);
CREATE INDEX idx_screen_categories_screen ON screen_categories(screen_id);

-- 4. New table: slot_assignments. One row per (category, position) that has media.
--    Absence of a row = empty slot = skipped on playback.
CREATE TABLE slot_assignments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_category_id UUID NOT NULL REFERENCES screen_categories(id) ON DELETE CASCADE,
    slot_position      INTEGER NOT NULL CHECK (slot_position >= 1),
    media_id           UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    assigned_at        TIMESTAMPTZ DEFAULT now(),
    updated_at         TIMESTAMPTZ DEFAULT now(),
    UNIQUE(screen_category_id, slot_position)
);
CREATE INDEX idx_assignments_screen_category ON slot_assignments(screen_category_id);

-- 5. Backfill: for any screen that existed before this migration, create the 3
--    default categories with sensible default timings.
INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'MORNING',   TIME '09:00:00', TIME '12:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;

INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'AFTERNOON', TIME '12:00:00', TIME '17:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;

INSERT INTO screen_categories (screen_id, category, start_time, end_time)
SELECT id, 'EVENING',   TIME '17:00:00', TIME '22:00:00' FROM screens
ON CONFLICT (screen_id, category) DO NOTHING;

-- 6. Replace slot_id with category + slot_position in impression_logs.
ALTER TABLE impression_logs DROP COLUMN slot_id;
ALTER TABLE impression_logs
    ADD COLUMN category      VARCHAR(20) CHECK (category IN ('MORNING', 'AFTERNOON', 'EVENING')),
    ADD COLUMN slot_position INTEGER;
