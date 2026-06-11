-- V28__default_music_category_bgm.sql
-- 배경음악 기본 카테고리를 BGM으로 변경(신규 기능 출시 직후 기본값 보정).
-- 현재 'ALL' 행은 모두 자동 생성된 기본값이라 BGM으로 일괄 보정한다(아직 사용자 선택 ALL 없음).
-- H2 호환: 문장을 각각 분리한다.

ALTER TABLE member_settings ALTER COLUMN music_category SET DEFAULT 'BGM';

UPDATE member_settings SET music_category = 'BGM' WHERE music_category = 'ALL';
