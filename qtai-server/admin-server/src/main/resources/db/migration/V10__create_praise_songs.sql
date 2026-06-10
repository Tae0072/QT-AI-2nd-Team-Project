-- V7__create_praise_songs.sql
CREATE TABLE praise_songs (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100)    NOT NULL,
    artist          VARCHAR(100),
    source_type     VARCHAR(20)     NOT NULL DEFAULT 'CURATED',
    license_note    VARCHAR(300),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
