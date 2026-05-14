-- 성경 66권 메타 + GEN 1:1 데모. (이지윤·이승욱 작업 시 정확한 한글 책명 채워 넣기)
INSERT INTO bible_books (book_code, name_kr, name_en, testament, ordinal, created_at, updated_at) VALUES
('GEN', '창세기',     'Genesis',     'OT', 1,  NOW(3), NOW(3)),
('EXO', '출애굽기',   'Exodus',      'OT', 2,  NOW(3), NOW(3)),
('LEV', '레위기',     'Leviticus',   'OT', 3,  NOW(3), NOW(3)),
('NUM', '민수기',     'Numbers',     'OT', 4,  NOW(3), NOW(3)),
('DEU', '신명기',     'Deuteronomy', 'OT', 5,  NOW(3), NOW(3)),
('PSA', '시편',       'Psalms',      'OT', 19, NOW(3), NOW(3)),
('PRO', '잠언',       'Proverbs',    'OT', 20, NOW(3), NOW(3)),
('ISA', '이사야',     'Isaiah',      'OT', 23, NOW(3), NOW(3)),
('MAT', '마태복음',   'Matthew',     'NT', 40, NOW(3), NOW(3)),
('MRK', '마가복음',   'Mark',        'NT', 41, NOW(3), NOW(3)),
('LUK', '누가복음',   'Luke',        'NT', 42, NOW(3), NOW(3)),
('JHN', '요한복음',   'John',        'NT', 43, NOW(3), NOW(3)),
('ROM', '로마서',     'Romans',      'NT', 45, NOW(3), NOW(3)),
('REV', '요한계시록', 'Revelation',  'NT', 66, NOW(3), NOW(3));

-- 데모용 1구절 (창세기 1:1)
INSERT INTO bible_kr_verses (book_id, chapter, verse, content, version)
SELECT id, 1, 1, '태초에 하나님이 천지를 창조하시니라', 'REVISED' FROM bible_books WHERE book_code = 'GEN';

INSERT INTO bible_en_verses (book_id, chapter, verse, content, version)
SELECT id, 1, 1, 'In the beginning God created the heaven and the earth.', 'KJV' FROM bible_books WHERE book_code = 'GEN';

-- 데모용 해설 — REFERENCE_SOURCE 1건(Matthew Henry 영문, 1:1-2:3 범위), GENERATED_EXPLANATION 1건(AI 한국어, 1:1 단일).
INSERT INTO bible_explanations
    (book_id, chapter_start, verse_start, chapter_end, verse_end,
     source_type, source, language, title, content, editor_verified_at, created_at, updated_at)
SELECT id, 1, 1, 2, 3,
       'REFERENCE_SOURCE', 'MATTHEW_HENRY', 'en',
       'The creation of the world',
       'The Bible begins with a magnificent declaration that God is the maker of all things... (PD)',
       NULL, NOW(3), NOW(3)
FROM bible_books WHERE book_code = 'GEN';

INSERT INTO bible_explanations
    (book_id, chapter_start, verse_start, chapter_end, verse_end,
     source_type, source, language, title, content, editor_verified_at, created_at, updated_at)
SELECT id, 1, 1, 1, 1,
       'GENERATED_EXPLANATION', 'AI_QT_KO', 'ko',
       '창조의 시작',
       '성경은 창조로 시작합니다. 모든 것이 하나님으로부터 시작되었음을 선언합니다.',
       NOW(3), NOW(3), NOW(3)
FROM bible_books WHERE book_code = 'GEN';

-- 오늘 QT 데모 스케줄 (2026-05-14, GEN 1:1)
INSERT INTO bible_today_qt_schedule
    (qt_date, book_id, chapter_start, verse_start, chapter_end, verse_end, source, created_at, updated_at)
SELECT DATE '2026-05-14', id, 1, 1, 1, 1, 'MANUAL', NOW(3), NOW(3) FROM bible_books WHERE book_code = 'GEN';
