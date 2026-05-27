package com.qtai.domain.note.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note_verses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Column(length = 500)
    private String highlight;

    private NoteVerse(Long noteId, Long bibleVerseId, short displayOrder) {
        this.noteId = noteId;
        this.bibleVerseId = bibleVerseId;
        this.displayOrder = displayOrder;
    }

    public static NoteVerse of(Long noteId, Long bibleVerseId, int displayOrder) {
        return new NoteVerse(noteId, bibleVerseId, (short) displayOrder);
    }
}
