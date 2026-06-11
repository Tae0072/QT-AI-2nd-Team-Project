CREATE TABLE journal_events (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BINARY(16)      NOT NULL,
    member_id           BIGINT          NOT NULL,
    note_id             BIGINT          NOT NULL,
    qt_passage_id       BIGINT,
    event_type          VARCHAR(30)     NOT NULL,
    previous_status     VARCHAR(10),
    current_status      VARCHAR(10)     NOT NULL,
    status              VARCHAR(10)     NOT NULL,
    occurred_at         TIMESTAMP       NOT NULL,
    processed_at        TIMESTAMP,
    failed_at           TIMESTAMP,
    last_error_message  VARCHAR(500),
    retry_count         INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    CONSTRAINT uk_journal_events_event_id UNIQUE (event_id),
    CONSTRAINT fk_journal_events_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_journal_events_note FOREIGN KEY (note_id) REFERENCES notes(id),
    CONSTRAINT fk_journal_events_qt_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id)
);

CREATE INDEX idx_journal_events_note ON journal_events (note_id);
CREATE INDEX idx_journal_events_status_occurred_at ON journal_events (status, occurred_at);
