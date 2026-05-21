package com.qtai.domain.qt.internal;

/**
 * QT 본문에 포함된 성경 절 매핑 엔티티.
 *
 * QtPassage 와 bible_verses 를 연결하는 중간 테이블.
 * 실제 절 텍스트는 bible 도메인에서 bookId·chapter·verse 로 조회한다.
 *
 * DDL 예시:
 *   CREATE TABLE qt_passage_verses (
 *       id              BIGINT AUTO_INCREMENT PRIMARY KEY,
 *       qt_passage_id   BIGINT NOT NULL,
 *       book_id         INT    NOT NULL,
 *       chapter         INT    NOT NULL,
 *       verse_start     INT    NOT NULL,
 *       verse_end       INT    NOT NULL,   -- 범위 끝 (단일 절이면 verse_start 와 동일)
 *       sort_order      INT    NOT NULL DEFAULT 0,
 *       FOREIGN KEY (qt_passage_id) REFERENCES qt_passages(id) ON DELETE CASCADE
 *   );
 */
// TODO: @Entity, @Table(name = "qt_passage_verses")
public class QtPassageVerse {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @ManyToOne(fetch = FetchType.LAZY)
    //        @JoinColumn(name = "qt_passage_id", nullable = false)
    //        QtPassage qtPassage;

    // TODO: @Column(nullable = false) Integer bookId;       — bible_books.id FK
    // TODO: @Column(nullable = false) Integer chapter;
    // TODO: @Column(nullable = false) Integer verseStart;   — 범위 시작 절
    // TODO: @Column(nullable = false) Integer verseEnd;     — 범위 끝 절

    // TODO: @Column(nullable = false) Integer sortOrder;    — 노출 순서
}
