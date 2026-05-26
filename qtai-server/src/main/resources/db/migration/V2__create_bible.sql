-- V2__create_bible.sql
CREATE TABLE bible_books (
    id              SMALLINT        PRIMARY KEY,
    testament       VARCHAR(10)     NOT NULL,
    code            VARCHAR(20)     NOT NULL UNIQUE,
    korean_name     VARCHAR(30)     NOT NULL,
    english_name    VARCHAR(50)     NOT NULL,
    display_order   SMALLINT        NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bible_verses (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    book_id     SMALLINT        NOT NULL,
    chapter     SMALLINT        NOT NULL,
    verse       SMALLINT        NOT NULL,
    krv_text    TEXT            NOT NULL,
    kjv_text    TEXT,
    INDEX idx_bible_verses_book_chapter (book_id, chapter),
    CONSTRAINT fk_bible_verses_book FOREIGN KEY (book_id) REFERENCES bible_books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
