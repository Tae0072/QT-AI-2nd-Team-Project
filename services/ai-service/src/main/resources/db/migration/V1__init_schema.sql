-- AI Service 초기 스키마. 02_ERD §4.

CREATE TABLE IF NOT EXISTS ai_prompt_templates (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    guide_step   VARCHAR(2)   NOT NULL,
    name         VARCHAR(100) NOT NULL,
    system_text  TEXT         NOT NULL,
    user_text    TEXT         NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3)  NOT NULL,
    updated_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_step_status (guide_step, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_sessions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    qt_date      DATE         NOT NULL,
    book_code    VARCHAR(8)   NOT NULL,
    chapter      INT          NOT NULL,
    verse_start  INT          NOT NULL,
    verse_end    INT          NOT NULL,
    guide_step   VARCHAR(2)   NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS',
    summary      TEXT         NULL,
    created_at   DATETIME(3)  NOT NULL,
    updated_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user_status (user_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_turns (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    session_id         BIGINT       NOT NULL,
    role               VARCHAR(16)  NOT NULL,    -- SYSTEM / USER / ASSISTANT
    guide_step         VARCHAR(2)   NULL,
    content            TEXT         NOT NULL,
    content_redacted   BOOLEAN      NOT NULL DEFAULT FALSE,
    prompt_template_id BIGINT       NULL,
    sources            JSON         NULL,         -- 구 rag_sources → sources (2026-05-14)
    created_at         DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_inbox_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    consumer_group  VARCHAR(128) NOT NULL,
    processed_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (idempotency_key, consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
