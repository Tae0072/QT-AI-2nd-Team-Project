-- 이승욱님 seed 실제데이터입력시 해당 더미데이터 삭제바람.
-- Flutter 에뮬레이터에서 오늘 QT 파싱/API 연동을 확인하기 위한 임시 더미 seed.
-- 실제 성경 번역 본문이 아니며, 금지 번역본 본문을 포함하지 않는다.
-- 대상 범위: bookCode=1CO, englishName=1 Corinthians, chapter=1, verses=10-17.

INSERT INTO qt_passages
    (qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at)
VALUES
    ('2026-06-02', 46, 1, 10, 17, '성서유니온 파싱 확인용 더미 QT', '고린도전서(1 Corinthians) 1:10-17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO bible_verses
    (book_id, chapter_no, verse_no, korean_text, english_text)
VALUES
    (46, 1, 10, '한글 테스트 본문 10', 'English dummy verse 10'),
    (46, 1, 11, '한글 테스트 본문 11', 'English dummy verse 11'),
    (46, 1, 12, '한글 테스트 본문 12', 'English dummy verse 12'),
    (46, 1, 13, '한글 테스트 본문 13', 'English dummy verse 13'),
    (46, 1, 14, '한글 테스트 본문 14', 'English dummy verse 14'),
    (46, 1, 15, '한글 테스트 본문 15', 'English dummy verse 15'),
    (46, 1, 16, '한글 테스트 본문 16', 'English dummy verse 16'),
    (46, 1, 17, '한글 테스트 본문 17', 'English dummy verse 17');
