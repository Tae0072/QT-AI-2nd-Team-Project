package com.qtai.domain.qt.internal;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "qt_passages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QtPassage extends BaseEntity {

    @Column(name = "qt_date", nullable = false, unique = true)
    private LocalDate qtDate;

    @Column(name = "book_id", nullable = false)
    private Short bookId;

    @Column(name = "end_book_id", nullable = false)
    private Short endBookId;

    @Column(name = "chapter", nullable = false)
    private Short chapter;

    @Column(name = "end_chapter", nullable = false)
    private Short endChapter;

    @Column(name = "start_verse", nullable = false)
    private Short startVerse;

    @Column(name = "end_verse", nullable = false)
    private Short endVerse;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "main_verse_ref", length = 100)
    private String mainVerseRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QtPassageStatus status = QtPassageStatus.PENDING_REVIEW;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    /** 시스템 배치가 성서유니온 범위를 실제로 가져온 시각(게시 시각과 별개). */
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    /**
     * 자동수집 메타 기록 — 수집 시각은 매 수집마다 갱신하고, 게시 시각은 아직 비어 있을 때만 설정한다.
     * {@code publishedAtIfAbsent}가 {@code null}이면 게시 시각은 건드리지 않는다(관리자 검토 대기 유지).
     */
    public void recordCollected(LocalDateTime collectedAt, LocalDateTime publishedAtIfAbsent) {
        this.collectedAt = collectedAt;
        if (publishedAtIfAbsent != null && this.publishedAt == null) {
            this.publishedAt = publishedAtIfAbsent;
        }
    }

    /**
     * 자동수집 신규 본문을 '미게시(검토 대기)'로 둔다 — 게시 시각(QT 날짜 04:00 KST)에 자동게시 스케줄러가
     * 노출시킨다. 수집 즉시 노출하지 않고 공개 정책(00:00~04:00 이전 캐시, §6)을 지키기 위함.
     */
    public void scheduleForAutoPublish() {
        this.status = QtPassageStatus.PENDING_REVIEW;
        this.publishedAt = null;
    }

    /** 단일 권·장 범위 생성. 시작=종료로 저장한다. */
    public static QtPassage create(
            LocalDate qtDate,
            Short bookId,
            Short chapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        return create(qtDate, bookId, bookId, chapter, chapter, startVerse, endVerse, title, mainVerseRef);
    }

    /** 권·장 교차 범위 생성. */
    public static QtPassage create(
            LocalDate qtDate,
            Short bookId,
            Short endBookId,
            Short chapter,
            Short endChapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        QtPassage passage = new QtPassage();
        passage.qtDate = qtDate;
        passage.updateRange(bookId, endBookId, chapter, endChapter, startVerse, endVerse, title, mainVerseRef);
        passage.status = QtPassageStatus.PENDING_REVIEW;
        return passage;
    }

    /** 단일 권·장 범위 갱신. 시작=종료로 저장한다. */
    public void updateRange(
            Short bookId,
            Short chapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        updateRange(bookId, bookId, chapter, chapter, startVerse, endVerse, title, mainVerseRef);
    }

    /** 권·장 교차 범위 갱신. */
    public void updateRange(
            Short bookId,
            Short endBookId,
            Short chapter,
            Short endChapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        this.bookId = bookId;
        this.endBookId = endBookId;
        this.chapter = chapter;
        this.endChapter = endChapter;
        this.startVerse = startVerse;
        this.endVerse = endVerse;
        this.title = title;
        this.mainVerseRef = mainVerseRef;
    }

    public void updateAdminRange(
            LocalDate qtDate,
            Short bookId,
            Short chapter,
            Short endChapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        this.qtDate = qtDate;
        // 관리자 수정은 같은 권 기준 → 종료 권 = 시작 권.
        updateRange(bookId, bookId, chapter, endChapter, startVerse, endVerse, title, mainVerseRef);
    }

    public void publish(LocalDateTime publishedAt) {
        if (this.status != QtPassageStatus.PENDING_REVIEW && this.status != QtPassageStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = QtPassageStatus.ACTIVE;
        this.publishedAt = publishedAt;
        this.hiddenAt = null;
    }

    public void hide(LocalDateTime hiddenAt) {
        if (this.status != QtPassageStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = QtPassageStatus.HIDDEN;
        this.hiddenAt = hiddenAt;
    }
}
