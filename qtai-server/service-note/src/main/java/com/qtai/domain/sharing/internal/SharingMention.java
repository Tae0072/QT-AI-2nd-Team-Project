package com.qtai.domain.sharing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 나눔 글/댓글의 '#닉네임' 멘션(태그) 기록.
 *
 * <p>멘션은 닉네임 텍스트가 아니라 <b>사람(mentionedMemberId)</b>으로 저장한다 — 닉네임이 바뀌어도
 * "내가 태그된 글" 목록·알림 대상이 정확하다. commentId가 있으면 댓글 멘션, null이면 게시글 본문 멘션이다.
 * createdAt은 공통 Clock(Asia/Seoul) 기준 시각을 Service가 주입한다.
 */
@Entity
@Table(name = "sharing_mentions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharingMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sharing_post_id", nullable = false)
    private Long sharingPostId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "mentioned_member_id", nullable = false)
    private Long mentionedMemberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 멘션 한 건 생성. 생성자가 protected라(JPA 전용) 외부는 이 팩토리로만 만든다.
     *
     * @param commentId 댓글 멘션이면 댓글 id, 게시글 본문 멘션이면 null
     */
    public static SharingMention of(Long sharingPostId, Long commentId, Long mentionedMemberId,
                                    LocalDateTime createdAt) {
        SharingMention mention = new SharingMention();
        mention.sharingPostId = sharingPostId;
        mention.commentId = commentId;
        mention.mentionedMemberId = mentionedMemberId;
        mention.createdAt = createdAt;
        return mention;
    }
}
