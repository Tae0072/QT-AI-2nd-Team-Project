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

    @Column(name = "chapter", nullable = false)
    private Short chapter;

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

    public static QtPassage create(
            LocalDate qtDate,
            Short bookId,
            Short chapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        QtPassage passage = new QtPassage();
        passage.qtDate = qtDate;
        passage.updateRange(bookId, chapter, startVerse, endVerse, title, mainVerseRef);
        passage.status = QtPassageStatus.PENDING_REVIEW;
        return passage;
    }

    public void updateRange(
            Short bookId,
            Short chapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        this.bookId = bookId;
        this.chapter = chapter;
        this.startVerse = startVerse;
        this.endVerse = endVerse;
        this.title = title;
        this.mainVerseRef = mainVerseRef;
    }

    public void updateAdminRange(
            LocalDate qtDate,
            Short bookId,
            Short chapter,
            Short startVerse,
            Short endVerse,
            String title,
            String mainVerseRef
    ) {
        this.qtDate = qtDate;
        updateRange(bookId, chapter, startVerse, endVerse, title, mainVerseRef);
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
