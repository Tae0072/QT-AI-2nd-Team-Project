package com.qtai.domain.bible.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bible_verses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bible_verse_coord",
                columnNames = {"book_id", "chapter_no", "verse_no"}
        ),
        indexes = @Index(name = "idx_bible_verses_book_chapter", columnList = "book_id, chapter_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BibleVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BibleBook book;

    @Column(name = "chapter_no", nullable = false)
    private Short chapterNo;

    @Column(name = "verse_no", nullable = false)
    private Short verseNo;

    @Column(name = "korean_text", nullable = false, columnDefinition = "TEXT")
    private String koreanText;

    @Column(name = "english_text", columnDefinition = "TEXT")
    private String englishText;
}
