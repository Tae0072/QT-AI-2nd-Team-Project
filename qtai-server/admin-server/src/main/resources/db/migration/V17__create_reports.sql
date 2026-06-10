-- V17__create_reports.sql
-- report 도메인 엔티티(Report) 누락 마이그레이션 보강 (PR #140).
-- ERD §2.18 reports. 대상은 (target_type, target_id) 다형 참조.

CREATE TABLE reports (
    id                     BIGINT       AUTO_INCREMENT PRIMARY KEY,
    reporter_member_id     BIGINT       NOT NULL,
    target_type            VARCHAR(30)  NOT NULL,
    target_id              BIGINT       NOT NULL,
    reason                 VARCHAR(50)  NOT NULL,
    detail                 TEXT,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    processed_by_admin_id  BIGINT,
    processed_at           DATETIME,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME,
    INDEX idx_reports_target (target_type, target_id),
    INDEX idx_reports_status_created (status, created_at),
    INDEX idx_reports_reporter (reporter_member_id),
    UNIQUE KEY uk_reports_reporter_target (reporter_member_id, target_type, target_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_member_id) REFERENCES members(id),
    CONSTRAINT fk_reports_admin FOREIGN KEY (processed_by_admin_id) REFERENCES admin_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
