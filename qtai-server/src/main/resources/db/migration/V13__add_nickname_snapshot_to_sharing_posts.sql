-- V13__add_nickname_snapshot_to_sharing_posts.sql
-- 나눔 글 작성자 닉네임 박제(07 §F-10)와 본문 범위 라벨(04 §4.4.1) 컬럼 추가.
-- 발행 로직(PublishNoteUseCase)이 채우기 전까지 기존 행을 막지 않도록 nullable로 추가한다.
-- H2 호환: ADD COLUMN 여러 개를 콤마로 이어 쓸 수 없으므로 각각 분리
ALTER TABLE sharing_posts
    ADD COLUMN nickname_snapshot VARCHAR(20) NULL;

ALTER TABLE sharing_posts
    ADD COLUMN snapshot_verse_label VARCHAR(100) NULL;
