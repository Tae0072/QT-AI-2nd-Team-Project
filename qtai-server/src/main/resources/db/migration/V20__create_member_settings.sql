-- V20__create_member_settings.sql
-- 사용자 설정 테이블. 알림 수신 ON/OFF + 폰트 크기(SMALL/MEDIUM/LARGE).
-- 첫 조회 시 기본값으로 자동 생성 (GET /api/v1/me/settings).

CREATE TABLE IF NOT EXISTS member_settings (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    member_id              BIGINT       NOT NULL,
    notification_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    font_size              VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_settings_member_id UNIQUE (member_id),
    CONSTRAINT fk_member_settings_member FOREIGN KEY (member_id) REFERENCES members(id)
);
