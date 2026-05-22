-- V4__create_notes.sql
CREATE TABLE notes (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT          NOT NULL,
    qt_passage_id       BIGINT,
    category            VARCHAR(20)     NOT NULL,
    status              VARCHAR(10)     NOT NULL DEFAULT 'DRAFT',
    title               VARCHAR(200)    NOT NULL,
    body                TEXT            NOT NULL,
    active_unique_key   VARCHAR(10),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notes_member (member_id),
    INDEX idx_notes_qt_passage (qt_passage_id),
    UNIQUE KEY uk_notes_meditation_active (member_id, qt_passage_id, active_unique_key),
    CONSTRAINT fk_notes_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_notes_qt_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE note_verses (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    note_id         BIGINT          NOT NULL,
    bible_verse_id  BIGINT          NOT NULL,
    display_order   SMALLINT        NOT NULL,
    highlight       VARCHAR(500),
    CONSTRAINT fk_nv_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT fk_nv_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
