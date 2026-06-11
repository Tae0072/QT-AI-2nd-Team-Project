-- V19__seed_sample_mission_definitions.sql
-- MVP 샘플 미션 정의(ACTIVE, MONTHLY). 진행률 배치(MissionProgressCalculator)가 계산할 대상.
-- metric_type은 note 월간 집계(savedDays / savedNoteCount / meditationStreakDays)와 1:1 매핑된다.
-- 시드 3종: MONTHLY_MEDITATION_DAYS(묵상일수 20), MONTHLY_NOTE_COUNT(저장노트 30), MONTHLY_STREAK_7(연속 7).
-- 금지 데이터 없음(번역본 본문/가사/시크릿 무관). 모두 ACTIVE·MONTHLY로 현재 배치 계산 범위에 포함된다.

INSERT INTO mission_definitions (code, title, metric_type, period_type, target_count, status, created_at)
VALUES
    ('MONTHLY_MEDITATION_DAYS', '이 달 묵상 20일', 'MEDITATION_SAVED_DAYS', 'MONTHLY', 20, 'ACTIVE', CURRENT_TIMESTAMP),
    ('MONTHLY_NOTE_COUNT',      '이 달 노트 30개', 'NOTE_SAVED_COUNT',      'MONTHLY', 30, 'ACTIVE', CURRENT_TIMESTAMP),
    ('MONTHLY_STREAK_7',        '연속 묵상 7일',   'STREAK_DAYS',           'MONTHLY', 7,  'ACTIVE', CURRENT_TIMESTAMP);
