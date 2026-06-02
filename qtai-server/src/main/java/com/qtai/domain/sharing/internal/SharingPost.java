package com.qtai.domain.sharing.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sharing_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharingPost extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** ERD §2.15 — 노트 1:1 공개 정책. 동일 노트의 중복 공유를 방지한다. */
    @Column(name = "note_id", nullable = false, unique = true)
    private Long noteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharingPostStatus status;

    // ── 스냅샷 (공유 시점의 노트 사본, 원본 변경에 영향받지 않음) ──
    @Column(name = "snapshot_title", nullable = false, length = 200)
    private String snapshotTitle;

    @Column(name = "snapshot_body", nullable = false, columnDefinition = "TEXT")
    private String snapshotBody;

    @Column(name = "snapshot_category", nullable = false, length = 20)
    private String snapshotCategory;

    @Column(name = "snapshot_qt_date")
    private java.time.LocalDate snapshotQtDate;

    /** 발행 시점 작성자 닉네임 박제 (07 §F-10). 닉네임 변경·노트 수정에 영향받지 않는다. */
    @Column(name = "nickname_snapshot", length = 20)
    private String nicknameSnapshot;

    /** 발행 시점 본문 범위 라벨 (예: "창세기 1:1-5", 04 §4.4.1 verseSnapshot.rangeLabel). */
    @Column(name = "snapshot_verse_label", length = 100)
    private String snapshotVerseLabel;

    // ── 댓글 허용 토글 ──
    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled = true;

    // ── 집계 ──
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    // ── 상태 변경 시각 ──
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "source_note_unshared_at")
    private LocalDateTime sourceNoteUnsharedAt;

    /**
     * 노트를 나눔으로 공개할 때 호출하는 정적 팩토리.
     *
     * <p>공개 "시점"의 값을 그대로 복사(스냅샷)해 둔다 — 이후 원본 노트나 닉네임이 바뀌어도
     * 이 게시글은 변하지 않는다(07 §F-10 공유본 스냅샷 정책).
     *
     * @param commentsEnabled 댓글 허용 여부(요청이 생략하면 호출부에서 true로 넘긴다)
     */
    public static SharingPost publish(Long memberId,
                                      Long noteId,
                                      String snapshotTitle,
                                      String snapshotBody,
                                      String snapshotCategory,
                                      java.time.LocalDate snapshotQtDate,
                                      String snapshotVerseLabel,
                                      String nicknameSnapshot,
                                      boolean commentsEnabled) {
        SharingPost post = new SharingPost();
        post.memberId = memberId;
        post.noteId = noteId;
        post.status = SharingPostStatus.PUBLISHED;
        // snapshot_title / snapshot_body 는 NOT NULL 컬럼이라 null이면 빈 문자열로 방어한다.
        post.snapshotTitle = snapshotTitle == null ? "" : snapshotTitle;
        post.snapshotBody = snapshotBody == null ? "" : snapshotBody;
        post.snapshotCategory = snapshotCategory;
        post.snapshotQtDate = snapshotQtDate;             // QT 노트가 아니면 null 가능(nullable)
        post.snapshotVerseLabel = snapshotVerseLabel;     // 절 라벨 없으면 null 가능(nullable)
        post.nicknameSnapshot = nicknameSnapshot;
        post.commentsEnabled = commentsEnabled;
        post.likeCount = 0;
        post.commentCount = 0;
        return post;
    }

    /**
     * 좋아요 수를 실제 {@code post_likes} 행 수로 맞춘다(COUNT 재계산 방식).
     * 관리 상태 엔티티에 호출하면 dirty checking으로 {@code like_count} UPDATE가 나간다.
     */
    public void syncLikeCount(long count) {
        this.likeCount = (int) count;
    }

    /**
     * 댓글 수를 실제 {@code comments}(삭제 안 된) 행 수로 맞춘다(COUNT 재계산 방식).
     * 관리 상태 엔티티에 호출하면 dirty checking으로 {@code comment_count} UPDATE가 나간다.
     */
    public void syncCommentCount(long count) {
        this.commentCount = (int) count;
    }

    /**
     * 나눔 게시글을 삭제한다(soft delete, 04 §4.4.6). {@code status=DELETED} + {@code deletedAt} 기록.
     * PUBLISHED·HIDDEN 어느 상태에서도 삭제 가능하다. 이미 삭제된 글의 멱등 처리는 서비스가 담당한다.
     */
    public void delete(LocalDateTime now) {
        this.status = SharingPostStatus.DELETED;
        markDeletedAt(now);
    }

    /**
     * 나눔 게시글을 숨긴다(공개 중단, 04 §4.4.6). PUBLISHED → HIDDEN, {@code hiddenAt} 기록.
     * 삭제된(DELETED) 글은 숨길 수 없다. 이미 숨겨진 글의 멱등 처리는 서비스가 담당한다.
     */
    public void hide(LocalDateTime now) {
        if (this.status == SharingPostStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SharingPostStatus.HIDDEN;
        this.hiddenAt = now;
    }

    /**
     * 숨긴 게시글을 다시 공개한다(되돌리기, 04 §4.4.6). HIDDEN → PUBLISHED, {@code hiddenAt}을 비운다.
     * 삭제된(DELETED) 글은 되돌릴 수 없다. 이미 공개 상태인 글의 멱등 처리는 서비스가 담당한다.
     */
    public void show() {
        if (this.status == SharingPostStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = SharingPostStatus.PUBLISHED;
        this.hiddenAt = null;
    }
}
