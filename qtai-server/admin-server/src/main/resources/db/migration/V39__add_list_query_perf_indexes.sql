-- 목록(전체) 조회 성능 개선용 인덱스.
-- 스키마는 admin-server가 단독 소유하므로 이 파일만 추가한다(CLAUDE.md §admin-sync).
-- MySQL 8.0 / 테스트 H2 모두 호환되도록 부분(WHERE)·DESC 키워드 없는 일반 복합 인덱스로 둔다.
-- (정렬이 DESC여도 옵티마이저가 인덱스를 역방향 스캔할 수 있다.)

-- 1) 나눔 피드: GET /api/v1/sharing-posts
--    WHERE status = 'PUBLISHED' ORDER BY created_at DESC (publishedAt→createdAt 매핑).
--    기존 인덱스는 (member_id) 뿐이라 발행글 필터+정렬이 풀스캔이었다.
CREATE INDEX idx_sharing_posts_status_created
    ON sharing_posts (status, created_at);

-- 2) 노트 목록: GET /api/v1/notes
--    WHERE member_id = ? [AND category/status] ORDER BY updated_at DESC.
--    '전체' 조회(필터 없음) 시 member 필터 후 updated_at 정렬을 인덱스로 처리한다.
CREATE INDEX idx_notes_member_updated
    ON notes (member_id, updated_at);
