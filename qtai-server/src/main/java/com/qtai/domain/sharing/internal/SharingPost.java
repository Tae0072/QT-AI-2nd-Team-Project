package com.qtai.domain.sharing.internal;

import com.qtai.common.entity.BaseEntity;
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

    @Column(name = "note_id", nullable = false)
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

    // ── 댓글 허용 토글 ──
    @Column(name = "comments_enabled", nullable = false)
    private Boolean commentsEnabled = true;

    // ── 집계 ──
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    // ── 상태 변경 시각 ──
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "source_note_unshared_at")
    private LocalDateTime sourceNoteUnsharedAt;
}