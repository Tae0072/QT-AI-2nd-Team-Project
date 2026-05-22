-- V1__create_members.sql
CREATE TABLE members (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    kakao_id            BIGINT          NOT NULL UNIQUE,
    email               VARCHAR(100),
    nickname            VARCHAR(20)     NOT NULL UNIQUE,
    profile_image_url   VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    role                VARCHAR(10)     NOT NULL DEFAULT 'USER',
    nickname_changed_at DATETIME,
    withdrawn_at        DATETIME,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
