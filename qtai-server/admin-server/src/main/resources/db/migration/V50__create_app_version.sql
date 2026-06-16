-- V50__create_app_version.sql
-- 앱 버전/업데이트 관리 (appversion 도메인, 2026-06-14 Lead 승인).
--  * app_version_state: 단일 행. 콘텐츠 버전(즉시 게시)과 앱 출시 버전(재설치 필요)을 함께 보관.
--  * pending_app_updates: 앱 재설치가 필요한 '업데이트 예정' 항목 큐.
-- H2(테스트)/MySQL 공용 문법만 사용한다(columnDefinition·ENUM 미사용, enum은 VARCHAR).

CREATE TABLE app_version_state (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    content_version         VARCHAR(40)     NOT NULL,
    app_version             VARCHAR(40)     NOT NULL,
    min_supported_version   VARCHAR(40)     NOT NULL,
    update_mode             VARCHAR(20)     NOT NULL DEFAULT 'NONE',  -- NONE|RECOMMENDED|FORCED
    update_message          VARCHAR(300),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP
);

-- 초기 버전 1행 시드(0.1.0).
INSERT INTO app_version_state
    (content_version, app_version, min_supported_version, update_mode, created_at, updated_at)
VALUES
    ('0.1.0', '0.1.0', '0.1.0', 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE pending_app_updates (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(150)    NOT NULL,
    description         VARCHAR(1000),
    target_app_version  VARCHAR(40)     NOT NULL,
    update_mode         VARCHAR(20)     NOT NULL DEFAULT 'RECOMMENDED',  -- NONE|RECOMMENDED|FORCED
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',      -- PENDING|APPLIED
    applied_at          TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP
);

CREATE INDEX idx_pending_app_updates_status ON pending_app_updates (status, created_at);
