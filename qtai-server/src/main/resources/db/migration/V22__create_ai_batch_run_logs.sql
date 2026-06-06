CREATE TABLE ai_batch_run_logs (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    batch_name      VARCHAR(80)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    created_count   INT          NOT NULL DEFAULT 0,
    failed_count    INT          NOT NULL DEFAULT 0,
    processed_count INT          NOT NULL DEFAULT 0,
    error_type      VARCHAR(100),
    error_message   VARCHAR(1000),
    started_at      TIMESTAMP    NOT NULL,
    finished_at     TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_batch_run_logs_batch_created ON ai_batch_run_logs (batch_name, created_at);
CREATE INDEX idx_ai_batch_run_logs_status_created ON ai_batch_run_logs (status, created_at);
