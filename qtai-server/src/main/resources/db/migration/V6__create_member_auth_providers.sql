-- V6__create_member_auth_providers.sql
CREATE TABLE member_auth_providers (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id         BIGINT       NOT NULL,
    provider          VARCHAR(20)  NOT NULL DEFAULT 'KAKAO',
    provider_user_id  VARCHAR(100) NOT NULL,
    connected_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_auth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auth_member_id ON member_auth_providers(member_id);
