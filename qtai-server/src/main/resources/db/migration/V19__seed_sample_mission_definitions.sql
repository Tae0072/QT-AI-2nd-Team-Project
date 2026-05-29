-- V19__seed_sample_mission_definitions.sql
-- MVP 샘플 미션 정의(ACTIVE, MONTHLY). 진행률 배치가 계산할 대상.
-- metric_type은 note 월간 집계(savedDays / savedNoteCount / meditationStreakDays)와 1:1 매핑된다.

INSERT INTO mission_definitions (code, title, metric_type, period_type, target_count, status, created_at)
VALUES
    ('MONTHLY_MEDITATION_DAYS', '이 달 묵상 20일', 'MEDITATION_SAVED_DAYS', 'MONTHLY', 20, 'ACTIVE', CURRENT_TIMESTAMP),
    ('MONTHLY_NOTE_COUNT',      '이 달 노트 30개', 'NOTE_SAVED_COUNT',      'MONTHLY', 30, 'ACTIVE', CURRENT_TIMESTAMP),
    ('MONTHLY_STREAK_7',        '연속 묵상 7일',   'STREAK_DAYS',           'MONTHLY', 7,  'ACTIVE', CURRENT_TIMESTAMP);
