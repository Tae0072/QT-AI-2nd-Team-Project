package com.qtai.domain.qt.internal;

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
@Table(name = "qt_passage_verses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QtPassageVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qt_passage_id", nullable = false)
    private Long qtPassageId;

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;
}
