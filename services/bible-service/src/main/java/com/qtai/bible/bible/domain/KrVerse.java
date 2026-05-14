package com.qtai.bible.bible.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한국어 성경 (개역한글). 02_ERD §3.3.
 *
 * <p>저작권: 비상업·교육 목적 + 출처 표기 필수. 개역개정·새번역 사용 금지.
 */
@Entity
@Table(
        name = "bible_kr_verses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "chapter", "verse", "version"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KrVerse {

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
    private String version;  // REVISED (개역한글)
}
