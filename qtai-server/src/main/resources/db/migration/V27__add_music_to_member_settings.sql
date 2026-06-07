-- V27__add_music_to_member_settings.sql
-- 배경음악 사용자 설정 컬럼 추가. 기본 ON.
-- (앱 전역 배경음악 기능, 2026-06-07 Lead 승인 — workflows/2026-06-07_app-background-music.md)
--
-- music_enabled : 배경음악 켜기/끄기 (기본 TRUE)
-- music_volume  : 0~100 볼륨 (기본 70)
-- music_category: 재생 대상 ALL | BGM | HYMN (기본 ALL)
--
-- H2 호환: ADD COLUMN 여러 개를 콤마로 이어 쓸 수 없으므로 각각 분리한다.

ALTER TABLE member_settings
    ADD COLUMN music_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE member_settings
    ADD COLUMN music_volume INT NOT NULL DEFAULT 70;

ALTER TABLE member_settings
    ADD COLUMN music_category VARCHAR(10) NOT NULL DEFAULT 'ALL';
