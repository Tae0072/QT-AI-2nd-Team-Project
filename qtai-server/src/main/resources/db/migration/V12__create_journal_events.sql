CREATE TABLE journal_events (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(36)     NOT NULL,
    event_type      VARCHAR(30)     NOT NULL,
    member_id       BIGINT          NOT NULL,
    note_id         BIGINT          NOT NULL,
    qt_passage_id   BIGINT,
    category        VARCHAR(20)     NOT NULL,
    previous_status VARCHAR(10),
    next_status     VARCHAR(10)     NOT NULL,
    saved_date      DATE,
    occurred_at     TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    CONSTRAINT uk_journal_events_event_id UNIQUE (event_id),
    CONSTRAINT fk_journal_events_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_journal_events_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT fk_journal_events_qt_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id)
);

CREATE INDEX idx_journal_events_member_saved_date ON journal_events (member_id, saved_date);
CREATE INDEX idx_journal_events_note_occurred_at ON journal_events (note_id, occurred_at);
