-- V26__create_music_tracks.sql
-- 앱 전역 배경음악(브금/찬송가) 음원 저장 테이블.
--
-- 정책 변경 주의: 07 요구사항/25 기능명세/CLAUDE.md §8의 '음원 서버·DB 저장 금지'를
-- Lead(T) 승인으로 신규 music 도메인에 한정해 예외 허용한다
-- (2026-06-07, 로열티프리/직접제작 음원 한정. F-09 찬양 큐레이션 금지 규칙은 그대로 유지).
-- 상세: doc/workspaces/Lead_강태오/workflows/2026-06-07_app-background-music.md
--
-- 음원 바이트는 audio_data(LONGBLOB)에 저장한다. 목록 조회는 메타데이터만 SELECT 하고
-- 스트리밍 시에만 audio_data 를 읽어 목록 쿼리가 무거워지지 않게 한다.

CREATE TABLE IF NOT EXISTS music_tracks (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(150)  NOT NULL,
    category      VARCHAR(20)   NOT NULL DEFAULT 'BGM',          -- BGM | HYMN
    mime_type     VARCHAR(60)   NOT NULL DEFAULT 'audio/mpeg',
    byte_size     BIGINT        NOT NULL DEFAULT 0,
    duration_sec  INT           NULL,
    sort_order    INT           NOT NULL DEFAULT 0,
    enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    license_note  VARCHAR(300)  NULL,                            -- 출처/라이선스(로열티프리·직접제작 확인)
    audio_data    LONGBLOB      NOT NULL,                        -- 음원 바이트(곡당 수 MB 가정)
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_music_tracks_enabled_sort (enabled, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
