-- V8__seed_sample_qt_passages.sql
-- 개발/테스트용 샘플 QT 본문 3건 (2026-05-26 ~ 2026-05-28)

INSERT INTO qt_passages (qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at) VALUES
('2026-05-26', 1, 1, 1, 5, '태초에 하나님이', '창세기 1:1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2026-05-27', 19, 23, 1, 6, '여호와는 나의 목자시니', '시편 23:1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2026-05-28', 43, 3, 16, 21, '하나님이 세상을 이처럼 사랑하사', '요한복음 3:16', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
