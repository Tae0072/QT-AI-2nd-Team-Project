package com.qtai.domain.note.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** ACTIVE=현재 활성 묵상, NULL=삭제/교체됨. (member_id, qt_passage_id, active_unique_key) UK로 1일 1건 멱등 보장 */
    @Column(name = "active_unique_key", length = 10)
    private String activeUniqueKey;
}