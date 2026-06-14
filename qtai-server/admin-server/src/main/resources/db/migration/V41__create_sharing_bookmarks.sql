-- V41__create_sharing_bookmarks.sql
-- 나눔 게시글 저장(북마크). 한 회원이 같은 글을 중복 저장하지 못하도록 (sharing_post_id, member_id) UNIQUE.
-- 좋아요(post_likes)와 동일한 구조 — updatedAt 불필요, createdAt만 보관(저장한 시각, 저장 목록 정렬 기준).
CREATE TABLE sharing_bookmarks (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id     BIGINT          NOT NULL,
    member_id           BIGINT          NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sharing_bookmarks UNIQUE (sharing_post_id, member_id),
    CONSTRAINT fk_sb_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_sb_member FOREIGN KEY (member_id) REFERENCES members(id)
);

-- 저장 목록 조회: member_id로 내 저장 글을 최근 저장순(created_at DESC)으로 모은다.
CREATE INDEX idx_sharing_bookmarks_member ON sharing_bookmarks (member_id, created_at DESC);
