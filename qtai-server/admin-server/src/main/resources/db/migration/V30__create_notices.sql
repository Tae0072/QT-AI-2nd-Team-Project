CREATE TABLE notices (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    admin_user_id   BIGINT       NOT NULL,
    title           VARCHAR(100) NOT NULL,
    body            LONGTEXT     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    published_at    DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notices_status_published (status, published_at),
    INDEX idx_notices_admin_created (admin_user_id, created_at),
    CONSTRAINT fk_notices_admin_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
