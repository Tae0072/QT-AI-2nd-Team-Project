-- V5__create_sharing.sql
CREATE TABLE sharing_posts (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT          NOT NULL,
    note_id         BIGINT          NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED',
    snapshot_title      VARCHAR(200)    NOT NULL,
    snapshot_body       TEXT            NOT NULL,
    snapshot_category   VARCHAR(20)     NOT NULL,
    snapshot_qt_date    DATE,
    source_note_unshared_at DATETIME,
    comments_enabled    BOOLEAN         NOT NULL DEFAULT TRUE,
    like_count          INT             NOT NULL DEFAULT 0,
    comment_count       INT             NOT NULL DEFAULT 0,
    hidden_at           DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sharing_posts_member (member_id),
    CONSTRAINT fk_sp_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_sp_note FOREIGN KEY (note_id) REFERENCES notes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id     BIGINT          NOT NULL,
    member_id           BIGINT          NOT NULL,
    parent_id           BIGINT,
    body                VARCHAR(1000)   NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comments_post (sharing_post_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_comments_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_likes (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    sharing_post_id     BIGINT          NOT NULL,
    member_id           BIGINT          NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_likes (sharing_post_id, member_id),
    CONSTRAINT fk_pl_post FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id),
    CONSTRAINT fk_pl_member FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
