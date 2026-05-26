-- V6__create_verse_explanations.sql
CREATE TABLE verse_explanations (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    bible_verse_id      BIGINT          NOT NULL,
    summary             VARCHAR(300),
    explanation         TEXT            NOT NULL,
    source_label        VARCHAR(200)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    active_unique_key   VARCHAR(20),
    ai_asset_id         BIGINT,
    approved_at         DATETIME(6),
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_explanations_verse_status (bible_verse_id, status),
    INDEX idx_explanations_status (status),
    UNIQUE KEY uk_explanations_active_per_verse (bible_verse_id, active_unique_key),
    CONSTRAINT fk_verse_explanations_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
