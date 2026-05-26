-- V6__create_notifications.sql
CREATE TABLE notifications (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT          NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    title           VARCHAR(100)    NOT NULL,
    body            VARCHAR(500),
    notice_id       BIGINT,
    link_type       VARCHAR(30),
    link_id         BIGINT,
    event_key       VARCHAR(120),
    read_at         DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notifications_member_read_created (member_id, read_at, created_at DESC),
    UNIQUE KEY uk_notifications_member_event (member_id, event_key),
    CONSTRAINT fk_notifications_member FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
