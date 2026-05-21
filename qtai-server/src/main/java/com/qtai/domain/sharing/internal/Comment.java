package com.qtai.domain.sharing.internal;

/**
 * 나눔 게시글 댓글 엔티티.
 *
 * 작성자 본인만 삭제 가능하다 (DELETE /api/v1/comments/{commentId}).
 * SharingPost.commentsEnabled=false 이면 댓글 작성 불가.
 *
 * DDL 예시:
 *   CREATE TABLE comments (
 *       id              BIGINT AUTO_INCREMENT PRIMARY KEY,
 *       sharing_post_id BIGINT      NOT NULL,
 *       member_id       BIGINT      NOT NULL,
 *       content         TEXT        NOT NULL,
 *       created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *       deleted_at      DATETIME    NULL,
 *       FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id)
 *   );
 */
// TODO: @Entity, @Table(name = "comments")
public class Comment {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @ManyToOne(fetch = FetchType.LAZY)
    //        @JoinColumn(name = "sharing_post_id", nullable = false)
    //        SharingPost sharingPost;

    // TODO: @Column(nullable = false) Long memberId;    — 댓글 작성자 FK

    // TODO: @Column(columnDefinition = "TEXT", nullable = false)
    //        String content;

    // TODO: @CreationTimestamp LocalDateTime createdAt;
    // TODO: LocalDateTime deletedAt;    — 소프트 삭제
}
