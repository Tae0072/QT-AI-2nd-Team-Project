package com.qtai.domain.qt.internal;

import com.qtai.common.entity.BaseEntity;
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

/**
 * QT 본문 범위.
 *
 * <p>범위 모델: {@code bookId}/{@code chapter}/{@code startVerse}는 시작 권·장·절,
 * {@code endBookId}/{@code endChapter}/{@code endVerse}는 종료 권·장·절이다.
 * 같은 권·장 범위면 시작=종료. 성서유니온 매일성경은 같은 권 안에서 장을 넘기는 범위가
 * 흔하므로(예: 9:1-10:5) 종료 장을 별도로 보존한다. ({@code endBookId}는 권 교차 대비 컬럼이며
 * 현재 수집 소스에는 권 교차가 없어 항상 {@code bookId}와 같다.)
 */
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
    private QtPassageStatus status = QtPassageStatus.ACTIVE;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    /** 단일 권·장 범위 생성(수동 등록 등). 시작=종료로 저장한다. */
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
        return passage;
    }

    public void publish(LocalDateTime publishedAt) {
        this.status = QtPassageStatus.ACTIVE;
        this.publishedAt = publishedAt;
        this.hiddenAt = null;
    }

    public void hide(LocalDateTime hiddenAt) {
        this.status = QtPassageStatus.HIDDEN;
        this.hiddenAt = hiddenAt;
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
}
