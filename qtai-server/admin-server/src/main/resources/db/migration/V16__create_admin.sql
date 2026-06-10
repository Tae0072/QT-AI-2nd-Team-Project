-- V16__create_admin.sql
-- admin 도메인 엔티티(AdminUser, AdminActionLog) 누락 마이그레이션 보강.
-- 엔티티는 PR #134에서 추가됐으나 Flyway 마이그레이션이 빠져 validate 프로파일에서 기동 실패.

CREATE TABLE admin_users (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    admin_role  VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME,
    UNIQUE KEY uk_admin_users_member (member_id),
    CONSTRAINT fk_admin_users_member FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_action_log (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    admin_user_id  BIGINT       NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    target_type    VARCHAR(50)  NOT NULL,
    target_id      BIGINT,
    reason         VARCHAR(500),
    acted_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_action_admin (admin_user_id, acted_at),
    CONSTRAINT fk_admin_action_admin_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
