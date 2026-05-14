package com.qtai.bible.bible.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 영어 성경 (KJV, Public Domain). 02_ERD §3.4.
 */
@Entity
@Table(
        name = "bible_en_verses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "chapter", "verse", "version"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Integer chapter;

    @Column(nullable = false)
    private Integer verse;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 20, nullable = false)
    private String version;  // KJV
}
