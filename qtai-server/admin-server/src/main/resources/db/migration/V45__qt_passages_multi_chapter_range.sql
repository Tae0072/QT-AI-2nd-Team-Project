-- QT 본문 범위를 권/장 교차까지 표현하기 위한 컬럼 추가.
-- 스키마는 admin-server가 단독 소유한다(CLAUDE.md). qt_passages에 종료 권/장을 보탠다.
--
-- 배경: 성서유니온 매일성경은 같은 권 안에서 장을 넘기는 범위(예: 9:1-10:5)를 자주 내보내는데,
-- 기존 단일 `chapter` 모델이 이를 거부해 그런 날 본문 수집이 통째로 비었다(2026-06-15 수집 실패).
-- 기존 book_id/chapter는 '시작' 앵커로 유지하고 end_book_id/end_chapter를 추가한다.
-- end_book_id는 권 교차 대비 컬럼이며, 현재 수집 소스에는 권 교차가 없어 항상 book_id와 같다.
--
-- MySQL/H2 호환을 위해 컬럼 추가는 한 문장씩 두고, 기존 행은 시작값으로 백필한다(V40 패턴).

ALTER TABLE qt_passages ADD COLUMN end_book_id SMALLINT NULL;
ALTER TABLE qt_passages ADD COLUMN end_chapter SMALLINT NULL;

UPDATE qt_passages SET end_book_id = book_id WHERE end_book_id IS NULL;
UPDATE qt_passages SET end_chapter = chapter WHERE end_chapter IS NULL;

ALTER TABLE qt_passages MODIFY COLUMN end_book_id SMALLINT NOT NULL;
ALTER TABLE qt_passages MODIFY COLUMN end_chapter SMALLINT NOT NULL;

ALTER TABLE qt_passages
    ADD CONSTRAINT fk_qt_passages_end_book FOREIGN KEY (end_book_id) REFERENCES bible_books(id);
