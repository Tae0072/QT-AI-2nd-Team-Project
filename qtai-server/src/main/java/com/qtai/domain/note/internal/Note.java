package com.qtai.domain.note.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 노트 엔티티 — ERD §notes.
 *
 * <p>MEDITATION 카테고리는 (member_id, qt_passage_id, active_unique_key='ACTIVE') UK로
 * 하루 1건 멱등 보장. 노트 삭제/교체 시 active_unique_key=null로 전환한다.
 *
 * <p>{@link SQLRestriction} 으로 모든 조회(findById/findAll 포함)에 deleted_at IS NULL
 * 필터를 자동 적용한다. JpaRepository 기본 메서드를 통해 삭제된 노트가 노출되는 사고 차단.
 */
@Entity
@Table(name = "notes", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_notes_meditation_active",
                columnNames = {"member_id", "qt_passage_id", "active_unique_key"}
        )
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseEntity {

    /** active_unique_key 상수 — partial unique index 패턴 */
    public static final String ACTIVE_KEY = "ACTIVE";
    public static final String PRIVATE_VISIBILITY = "PRIVATE";

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

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * ACTIVE=현재 활성 묵상, NULL=삭제/교체됨.
     * (member_id, qt_passage_id, active_unique_key) UK로 1일 1건 멱등 보장.
     * CHAR(6) 수준이나 도메인 서비스에서 추가 상태값 확장 가능성을 고려해 VARCHAR(10) 유지.
     */
    @Column(name = "active_unique_key", length = 10)
    private String activeUniqueKey;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @Builder
    private Note(Long memberId, Long qtPassageId, NoteCategory category,
                 String title, String body) {
        this.memberId = memberId;
        this.qtPassageId = qtPassageId;
        this.category = category;
        this.status = NoteStatus.DRAFT;
        this.visibility = PRIVATE_VISIBILITY;
        this.title = title;
        this.body = body;
        // MEDITATION 카테고리는 반드시 active_unique_key='ACTIVE'로 생성
        this.activeUniqueKey = (category == NoteCategory.MEDITATION) ? ACTIVE_KEY : null;
    }

    public static Note sermon(Long memberId, String title, String body, NoteStatus status) {
        Note note = Note.builder()
                .memberId(memberId)
                .category(NoteCategory.SERMON)
                .title(title == null ? "" : title)
                .body(body == null ? "" : body)
                .build();
        note.status = status == null ? NoteStatus.SAVED : status;
        note.visibility = PRIVATE_VISIBILITY;
        note.qtPassageId = null;
        note.activeUniqueKey = null;
        note.savedAt = note.status == NoteStatus.SAVED ? LocalDateTime.now() : null;
        return note;
    }

    /** 노트 비활성화 — 삭제/교체 시 UK를 해제하여 새 묵상 노트 생성을 허용한다. */
    public void deactivate() {
        this.activeUniqueKey = null;
    }
}

