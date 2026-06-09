package com.qtai.domain.sharing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좋아요 엔티티. updatedAt이 불필요하므로 BaseEntity를 상속하지 않고
 * createdAt만 보관한다. createdAt은 공통 Clock(Asia/Seoul) 기준 시각을 Service가 주입한다.
 */
@Entity
@Table(name = "post_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sharing_post_id", "member_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sharing_post_id", nullable = false)
    private Long sharingPostId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 좋아요 한 건 생성. 생성자가 protected라(JPA 전용) 외부는 이 팩토리로만 만든다.
     * createdAt은 공통 Clock(Asia/Seoul) 기준 시각을 호출자(Service)가 주입한다 — 엔티티에서
     * {@code LocalDateTime.now()}를 직접 부르지 않아 04:00 KST 등 시간 정책을 일관되게 유지한다.
     */
    public static PostLike of(Long sharingPostId, Long memberId, LocalDateTime createdAt) {
        PostLike like = new PostLike();
        like.sharingPostId = sharingPostId;
        like.memberId = memberId;
        like.createdAt = createdAt;
        return like;
    }
}
