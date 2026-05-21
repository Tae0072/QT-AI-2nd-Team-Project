package com.qtai.domain.sharing.internal;

/**
 * 나눔 게시글 좋아요 엔티티 (ERD §2.17 likes).
 *
 * ERD 테이블명: likes (not post_likes).
 * (sharing_post_id, member_id) UNIQUE 제약으로 중복 좋아요 방지.
 * 좋아요 취소는 DELETE /api/v1/sharing-posts/{postId}/like.
 *
 * DDL 예시:
 *   CREATE TABLE likes (
 *       id              BIGINT   AUTO_INCREMENT PRIMARY KEY,
 *       sharing_post_id BIGINT   NOT NULL,
 *       member_id       BIGINT   NOT NULL,
 *       created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 *       UNIQUE KEY uk_likes_post_member (sharing_post_id, member_id),
 *       INDEX idx_likes_member (member_id),
 *       FOREIGN KEY (sharing_post_id) REFERENCES sharing_posts(id)
 *   );
 */
// TODO: @Entity, @Table(name = "likes",
//        uniqueConstraints = @UniqueConstraint(
//            name = "uk_likes_post_member", columnNames = {"sharing_post_id", "member_id"}))
public class PostLike {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @ManyToOne(fetch = FetchType.LAZY)
    //        @JoinColumn(name = "sharing_post_id", nullable = false)
    //        SharingPost sharingPost;

    // TODO: @Column(nullable = false) Long memberId;

    // TODO: @CreationTimestamp LocalDateTime createdAt;
}
