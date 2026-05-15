-- Bible Service 초기 스키마. 02_ERD §3·§5.
-- Charset · Collation 표준 (02_ERD §8.4): utf8mb4 / utf8mb4_unicode_ci

-- ============================
-- Bible 도메인
-- ============================
CREATE TABLE IF NOT EXISTS bible_books (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    book_code    VARCHAR(8)   NOT NULL,
    name_kr      VARCHAR(50)  NOT NULL,
    name_en      VARCHAR(50)  NOT NULL,
    testament    VARCHAR(3)   NOT NULL,    -- OT / NT
    ordinal      INT          NOT NULL,
    created_at   DATETIME(3)  NOT NULL,
    updated_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_code (book_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bible_kr_verses (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    book_id   BIGINT      NOT NULL,
    chapter   INT         NOT NULL,
    verse     INT         NOT NULL,
    content   TEXT        NOT NULL,
    version   VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kr_passage (book_id, chapter, verse, version),
    KEY idx_kr_book_chap (book_id, chapter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bible_en_verses (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    book_id   BIGINT      NOT NULL,
    chapter   INT         NOT NULL,
    verse     INT         NOT NULL,
    content   TEXT        NOT NULL,
    version   VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_en_passage (book_id, chapter, verse, version),
    KEY idx_en_book_chap (book_id, chapter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- bible_explanations: REFERENCE_SOURCE(Tyndale/MHC/Bible Dictionary)와
-- GENERATED_EXPLANATION(AI 생성 한국어 해설)을 한 테이블에 보관하고 source_type 으로 구분.
-- 단일 절 row는 chapter_start == chapter_end, verse_start == verse_end 로 저장.
-- 범위가 있는 원천(예: Genesis 41:37-57)도 자연스럽게 들어간다.
CREATE TABLE IF NOT EXISTS bible_explanations (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    book_id            BIGINT       NOT NULL,
    chapter_start      INT          NOT NULL,
    verse_start        INT          NOT NULL,
    chapter_end        INT          NOT NULL,
    verse_end          INT          NOT NULL,
    source_type        VARCHAR(32)  NOT NULL,   -- REFERENCE_SOURCE / GENERATED_EXPLANATION
    source             VARCHAR(32)  NOT NULL,   -- TYNDALE / MATTHEW_HENRY / BIBLE_DICTIONARY / AI_QT_KO / DUMMY_KR
    language           VARCHAR(5)   NOT NULL,   -- ko / en
    title              VARCHAR(200),
    content            TEXT         NOT NULL,
    editor_verified_at DATETIME(3)  NULL,        -- GENERATED_EXPLANATION 만 의미 있음
    created_at         DATETIME(3)  NOT NULL,
    updated_at         DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_expl_passage_range (book_id, chapter_start, verse_start, chapter_end, verse_end),
    KEY idx_expl_source_type (source_type, editor_verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 오늘 QT 본문 스케줄. 하루 1행 (qt_date PK), 본문 길이 자유 — 한 절·단락·다중 장 모두 허용 (ADR-0021, 02_ERD v2.3).
-- 성서유니온 19:00 스크래퍼가 매일 1 row 적재 (예: 창세기 41:37-57 = 21절).
-- 한 절일 경우 chapter_start == chapter_end AND verse_start == verse_end 로 들어옴.
-- 유일한 좌표 제약은 (chapter_start, verse_start) <= (chapter_end, verse_end).
CREATE TABLE IF NOT EXISTS bible_today_qt_schedule (
    qt_date       DATE        NOT NULL,
    book_id       BIGINT      NOT NULL,
    chapter_start INT         NOT NULL,
    verse_start   INT         NOT NULL,
    chapter_end   INT         NOT NULL,
    verse_end     INT         NOT NULL,
    source        VARCHAR(32) NOT NULL,        -- SU_KR / MANUAL 등
    created_at    DATETIME(3) NOT NULL,
    updated_at    DATETIME(3) NOT NULL,
    PRIMARY KEY (qt_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================
-- Journal 도메인
-- ============================
-- MVP: 하루 1 QT 정책에 따라 (user_id, qt_date) UNIQUE 유지.
CREATE TABLE IF NOT EXISTS journal_journals (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    qt_date         DATE         NOT NULL,
    book_code       VARCHAR(8)   NOT NULL,
    chapter         INT          NOT NULL,
    verse_start     INT          NOT NULL,
    verse_end       INT          NOT NULL,
    felt            TEXT         NULL,
    memorable_verse TEXT         NULL,
    application     TEXT         NULL,
    prayer          TEXT         NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    ai_session_id   BIGINT       NULL,
    ai_summary      TEXT         NULL,
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_qt (user_id, qt_date),
    KEY idx_journal_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_events (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    journal_id  BIGINT      NOT NULL,
    sequence    BIGINT      NOT NULL,
    event_type  VARCHAR(64) NOT NULL,
    event_data  JSON        NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_seq (journal_id, sequence),
    KEY idx_journal_events_journal (journal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_shares (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    journal_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    visibility  VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC',
    deleted_at  DATETIME(3)  NULL,
    created_at  DATETIME(3)  NOT NULL,
    updated_at  DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_shares_user (user_id),
    KEY idx_shares_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- INBOX_KEYS — Kafka 컨슈머 멱등성 (02_ERD §8.3, AGENTS.md)
CREATE TABLE IF NOT EXISTS journal_inbox_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    consumer_group  VARCHAR(128) NOT NULL,
    processed_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (idempotency_key, consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
