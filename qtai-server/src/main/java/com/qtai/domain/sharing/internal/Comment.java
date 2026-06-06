package com.qtai.domain.sharing.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Column(name = "sharing_post_id", nullable = false)
    private Long sharingPostId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    /**
     * 댓글 한 건 생성(평면 댓글). 생성자가 protected라 외부는 이 팩토리로만 만든다.
     * v1은 대댓글이 없어 parentId는 null로 둔다. createdAt은 BaseEntity가 채운다.
     */
    public static Comment of(Long sharingPostId, Long memberId, String body) {
        Comment comment = new Comment();
        comment.sharingPostId = sharingPostId;
        comment.memberId = memberId;
        comment.body = body;
        return comment;
    }

    /** 소프트 삭제 — 행은 남기고 숨김 플래그만 켠다(목록 조회에서 제외). */
    public void markDeleted() {
        this.isDeleted = true;
    }
}

