package com.qtai.domain.qt.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /** 자동수집이 본문 범위를 실제로 가져온 시각. 게시 시각과 분리한다. */
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public void recordCollected(LocalDateTime collectedAt, LocalDateTime publishedAtIfAbsent) {
        this.collectedAt = collectedAt;
        if (publishedAtIfAbsent != null && this.publishedAt == null) {
            this.publishedAt = publishedAtIfAbsent;
        }
    }

    /** 자동수집 본문을 04:00 자동게시 전까지 미게시 상태로 예약한다. */
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
