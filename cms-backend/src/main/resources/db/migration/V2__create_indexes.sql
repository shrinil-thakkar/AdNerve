CREATE INDEX idx_slots_screen_id ON slots(screen_id);
CREATE INDEX idx_slot_assignments_slot_id ON slot_assignments(slot_id);
CREATE INDEX idx_impressions_screen_media ON impression_logs(screen_id, media_id, played_at);
CREATE INDEX idx_impressions_played_at ON impression_logs(played_at);
CREATE INDEX idx_screens_is_active ON screens(is_active);
CREATE INDEX idx_slots_is_active ON slots(screen_id, is_active);
