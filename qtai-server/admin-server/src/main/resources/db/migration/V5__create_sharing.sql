-- V5__create_sharing.sql
CREATE TABLE sharing_posts (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT          NOT NULL,
    note_id         BIGINT          NOT NULL UNIQUE,  -- ERD §2.15: 노트 1:1 공개
    status              VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED',
    snapshot_title      VARCHAR(200)    NOT NULL,
    snapshot_body       TEXT            NOT NULL,
    snapshot_category   VARCHAR(20)     NOT NULL,
    snapshot_qt_date    DATE,
    source_note_unshared_at TIMESTAMP,
    comments_enabled    BOOLEAN         NOT NULL DEFAULT TRUE,
    like_count          INT             NOT NULL DEFAULT 0,
    comment_count       INT             NOT NULL DEFAULT 0,
    hidden_at           TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_sp_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_sp_note FOREIGN KEY (note_id) REFERENCES notes(id)
);

CREATE INDEX idx_sharing_posts_member ON sharing_posts (member_id);

CREATE TABLE comments (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id     BIGINT          NOT NULL,
    member_id           BIGINT          NOT NULL,
    parent_id           BIGINT,
    body                VARCHAR(1000)   NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    CONSTRAINT fk_comments_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_comments_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id)
);

CREATE INDEX idx_comments_post ON comments (sharing_post_id);

CREATE TABLE post_likes (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id     BIGINT          NOT NULL,
    member_id           BIGINT          NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_likes UNIQUE (sharing_post_id, member_id),
    CONSTRAINT fk_pl_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_pl_member FOREIGN KEY (member_id) REFERENCES members(id)
);
