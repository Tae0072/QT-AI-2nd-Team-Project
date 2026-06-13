package com.qtai.domain.note.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notes", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_notes_meditation_active",
                columnNames = {"member_id", "qt_passage_id", "active_unique_key"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseEntity {

    public static final String ACTIVE_KEY = "ACTIVE";

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "qt_passage_id")
    private Long qtPassageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NoteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NoteVisibility visibility;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "remember_section", columnDefinition = "TEXT")
    private String rememberSection;

    @Column(name = "interpret_section", columnDefinition = "TEXT")
    private String interpretSection;

    @Column(name = "apply_section", columnDefinition = "TEXT")
    private String applySection;

    @Column(name = "pray_section", columnDefinition = "TEXT")
    private String praySection;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @Column(name = "active_unique_key", length = 10)
    private String activeUniqueKey;

    @Builder
    private Note(Long memberId, Long qtPassageId, NoteCategory category,
                 String title, String body) {
        this(memberId, qtPassageId, category, NoteStatus.DRAFT, NoteVisibility.PRIVATE,
                title, body, null, null, null, null, null);
    }

    private Note(Long memberId, Long qtPassageId, NoteCategory category, NoteStatus status,
                 NoteVisibility visibility, String title, String body, String rememberSection,
                 String interpretSection, String applySection, String praySection,
                 LocalDateTime now) {
        this.memberId = memberId;
        this.qtPassageId = qtPassageId;
        this.category = category;
        this.visibility = visibility;
        this.title = title;
        this.body = body;
        this.rememberSection = rememberSection;
        this.interpretSection = interpretSection;
        this.applySection = applySection;
        this.praySection = praySection;
        transitionTo(status, now);
    }

    public static Note create(Long memberId, Long qtPassageId, NoteCategory category, NoteStatus status,
                              NoteVisibility visibility, String title, String body, String rememberSection,
                              String interpretSection, String applySection, String praySection,
                              LocalDateTime now) {
        return new Note(memberId, qtPassageId, category, status, visibility, title, body,
                rememberSection, interpretSection, applySection, praySection, now);
    }

    public void update(NoteCategory category, Long qtPassageId, NoteStatus status, NoteVisibility visibility,
                       String title, String body, String rememberSection, String interpretSection,
                       String applySection, String praySection, LocalDateTime now) {
        if (this.status == NoteStatus.DELETED || getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        this.category = category;
        this.qtPassageId = qtPassageId;
        this.visibility = visibility;
        this.title = title;
        this.body = body;
        this.rememberSection = rememberSection;
        this.interpretSection = interpretSection;
        this.applySection = applySection;
        this.praySection = praySection;
        transitionTo(status, now);
    }

    /** 나눔 공개 시 노트를 SHARED로 표시한다(목록의 shared 플래그 근거). */
    public void markShared() {
        this.visibility = NoteVisibility.SHARED;
    }

    /** 나눔 공개 중단(삭제) 시 노트를 다시 PRIVATE로 되돌린다. */
    public void markUnshared() {
        this.visibility = NoteVisibility.PRIVATE;
    }

    public void delete(LocalDateTime now) {
        this.status = NoteStatus.DELETED;
        this.savedAt = null;
        this.activeUniqueKey = null;
        markDeletedAt(now);
    }

    public boolean isDeleted() {
        return status == NoteStatus.DELETED || getDeletedAt() != null;
    }

    private void transitionTo(NoteStatus nextStatus, LocalDateTime now) {
        this.status = nextStatus;
        if (nextStatus == NoteStatus.SAVED) {
            this.savedAt = now;
        } else if (nextStatus == NoteStatus.DRAFT) {
            this.savedAt = null;
        }
        refreshActiveUniqueKey();
    }

    private void refreshActiveUniqueKey() {
        this.activeUniqueKey = (category == NoteCategory.MEDITATION && status != NoteStatus.DELETED)
                ? ACTIVE_KEY
                : null;
    }
}
