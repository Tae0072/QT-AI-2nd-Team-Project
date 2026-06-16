-- V42__create_sharing_mentions.sql
-- 나눔 글/댓글의 '#닉네임' 멘션(태그) 기록. "내가 태그된 글" 목록과 멘션 알림의 근거가 된다.
-- 멘션은 사람(member_id)으로 저장하므로 닉네임이 바뀌어도 목록/알림 대상은 정확하다.
CREATE TABLE sharing_mentions (
    id                   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id      BIGINT       NOT NULL,            -- 멘션이 등장한(혹은 속한) 게시글 — 목록 기준
    comment_id           BIGINT       NULL,               -- 댓글 멘션이면 댓글 id, 게시글 본문 멘션이면 NULL
    mentioned_member_id  BIGINT       NOT NULL,           -- 태그된(멘션된) 회원
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sm_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_sm_comment FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_sm_member FOREIGN KEY (mentioned_member_id) REFERENCES members(id)
);

-- "내가 태그된 글" 조회: 멘션된 회원으로 최근순.
CREATE INDEX idx_sharing_mentions_member ON sharing_mentions (mentioned_member_id, created_at DESC);
-- 글 단위 멘션 조회/정리용.
CREATE INDEX idx_sharing_mentions_post ON sharing_mentions (sharing_post_id);
