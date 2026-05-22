-- V3__create_qt_passages.sql
CREATE TABLE qt_passages (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    qt_date         DATE            NOT NULL UNIQUE,
    book_id         SMALLINT        NOT NULL,
    chapter         SMALLINT        NOT NULL,
    start_verse     SMALLINT        NOT NULL,
    end_verse       SMALLINT        NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    main_verse_ref  VARCHAR(100),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_qt_passages_date (qt_date),
    CONSTRAINT fk_qt_passages_book FOREIGN KEY (book_id) REFERENCES bible_books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qt_passage_verses (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    qt_passage_id   BIGINT          NOT NULL,
    bible_verse_id  BIGINT          NOT NULL,
    display_order   SMALLINT        NOT NULL,
    CONSTRAINT fk_qpv_passage FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id),
    CONSTRAINT fk_qpv_verse FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
