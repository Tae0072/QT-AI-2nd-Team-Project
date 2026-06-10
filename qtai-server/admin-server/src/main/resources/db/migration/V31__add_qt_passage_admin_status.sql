-- qt_passages.qt_date uniqueness is already enforced by V3__create_qt_passages.sql
-- (`qt_date DATE NOT NULL UNIQUE`). This migration adds admin moderation columns only.
ALTER TABLE qt_passages
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    ADD COLUMN published_at TIMESTAMP NULL,
    ADD COLUMN hidden_at TIMESTAMP NULL;

CREATE INDEX idx_qt_passages_status_date ON qt_passages (status, qt_date);
