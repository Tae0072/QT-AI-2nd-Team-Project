-- 닉네임 변경 이력 (F-04/F-10 관리자 회원 상세). 닉네임이 바뀔 때마다 1행 추가(append-only).
-- 기록 주체는 service-user(원본), 조회는 admin-server. 스키마는 admin-server(Flyway) 단독 소유.
CREATE TABLE IF NOT EXISTS nickname_change_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    old_nickname  VARCHAR(20)  NULL,
    new_nickname  VARCHAR(20)  NOT NULL,
    changed_at    DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_nickname_history_member (member_id, changed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
