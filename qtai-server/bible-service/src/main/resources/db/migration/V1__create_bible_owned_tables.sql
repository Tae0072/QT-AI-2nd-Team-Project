-- V1__create_bible_owned_tables.sql
-- bible-service 소유 테이블(DB-per-service). 모놀리식 V2__create_bible.sql 기준.
-- 읽기 전용 참조 데이터(books/verses). 본문은 KJV/KRV(허용)만 — 개역개정·ESV·NIV 금지(CLAUDE.md §8).
-- 주의: study.glossary_terms.bible_verse_id → bible_verses 크로스 도메인 FK는 본 서비스에 두지 않는다(study 책임).
CREATE TABLE bible_books (
    id              SMALLINT        PRIMARY KEY,
    testament       VARCHAR(10)     NOT NULL,
    code            VARCHAR(20)     NOT NULL UNIQUE,
    korean_name     VARCHAR(30)     NOT NULL,
    english_name    VARCHAR(50)     NOT NULL,
    display_order   SMALLINT        NOT NULL UNIQUE
);

CREATE TABLE bible_verses (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    book_id         SMALLINT        NOT NULL,
    chapter_no      SMALLINT        NOT NULL,
    verse_no        SMALLINT        NOT NULL,
    korean_text     TEXT            NOT NULL,
    english_text    TEXT,
    UNIQUE KEY uk_bible_verse_coord (book_id, chapter_no, verse_no),
    CONSTRAINT fk_bible_verses_book FOREIGN KEY (book_id) REFERENCES bible_books(id)
);

CREATE INDEX idx_bible_verses_book_chapter ON bible_verses (book_id, chapter_no);
