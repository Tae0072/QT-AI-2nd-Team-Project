package com.qtai.domain.qt.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}
