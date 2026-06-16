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
 * 나눔 게시글 저장(북마크) 엔티티. 좋아요({@link PostLike})와 같은 구조로,
 * updatedAt이 불필요하므로 BaseEntity를 상속하지 않고 createdAt(저장한 시각)만 보관한다.
 * createdAt은 공통 Clock(Asia/Seoul) 기준 시각을 Service가 주입한다 — 저장 목록을
 * 최근 저장순으로 정렬하는 기준이 된다.
 */
@Entity
@Table(name = "sharing_bookmarks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sharing_post_id", "member_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharingBookmark {

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
     * 저장 한 건 생성. 생성자가 protected라(JPA 전용) 외부는 이 팩토리로만 만든다.
     * createdAt은 공통 Clock(Asia/Seoul) 기준 시각을 호출자(Service)가 주입한다.
     */
    public static SharingBookmark of(Long sharingPostId, Long memberId, LocalDateTime createdAt) {
        SharingBookmark bookmark = new SharingBookmark();
        bookmark.sharingPostId = sharingPostId;
        bookmark.memberId = memberId;
        bookmark.createdAt = createdAt;
        return bookmark;
    }
}
