package com.qtai.bible.journal.domain;

import com.qtai.bible.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 묵상 노트 read model. 02_ERD §5.2.
 *
 * <p>MVP 4필드 자동 저장: felt, memorableVerse, application, prayer.
 * 사용자 노출 글자 수 제한 없음. 저장 버튼 없음.
 * 오늘 QT DRAFT 멱등키 = userId + qtDate.
 *
 * <p>이벤트 소싱: 본 테이블은 read model이고 truth는 journal_events.
 */
@Entity
@Table(
        name = "journal_journals",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_qt_date", columnNames = {"user_id", "qt_date"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "qt_date", nullable = false)
    private LocalDate qtDate;

    @Column(name = "book_code", length = 8, nullable = false)
    private String bookCode;

    @Column(nullable = false)
    private Integer chapter;

    @Column(name = "verse_start", nullable = false)
    private Integer verseStart;

    @Column(name = "verse_end", nullable = false)
    private Integer verseEnd;     // MVP에서는 verseStart == verseEnd

    @Column(columnDefinition = "TEXT")
    private String felt;

    @Column(name = "memorable_verse", columnDefinition = "TEXT")
    private String memorableVerse;

    @Column(columnDefinition = "TEXT")
    private String application;

    @Column(columnDefinition = "TEXT")
    private String prayer;

    /** DRAFT / COMPLETED */
    @Column(length = 16, nullable = false)
    private String status = "DRAFT";

    /** AI 세션 완료 시 첨부 (ai.session.completed 컨슈머가 set) */
    @Column(name = "ai_session_id")
    private Long aiSessionId;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /**
     * 오늘 QT DRAFT 멱등 생성용 팩토리. MVP에서는 verseStart == verseEnd.
     * 사용처: {@code POST /api/v1/journals/today}.
     */
    public static Journal newTodayDraft(Long userId, LocalDate qtDate, String bookCode,
                                        Integer chapter, Integer verse) {
        Journal j = new Journal();
        j.userId = userId;
        j.qtDate = qtDate;
        j.bookCode = bookCode;
        j.chapter = chapter;
        j.verseStart = verse;
        j.verseEnd = verse;
        j.status = "DRAFT";
        return j;
    }
}
