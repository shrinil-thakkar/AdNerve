ALTER TABLE media ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';
CREATE INDEX idx_media_status ON media(status);
