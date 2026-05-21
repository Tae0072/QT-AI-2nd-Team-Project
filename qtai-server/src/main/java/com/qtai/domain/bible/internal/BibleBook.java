package com.qtai.domain.bible.internal;

/**
 * 성경 권(Book) 엔티티 — 구약 39권 + 신약 27권 = 66권.
 *
 * GET /api/v1/bible/books API 의 기반 데이터.
 * 허용 번역본: KRV(한글), KJV(영어). 개역개정·ESV·NIV 는 seed/fixture/response 에 사용 금지 (CLAUDE.md §8).
 *
 * ERD §2.3 bible_books 기준 컬럼명 사용.
 *
 * DDL 예시:
 *   CREATE TABLE bible_books (
 *       id           SMALLINT     PRIMARY KEY,       -- 1~66 고정 PK
 *       testament    VARCHAR(10)  NOT NULL,           -- OLD, NEW
 *       code         VARCHAR(20)  NOT NULL UNIQUE,    -- 예: GENESIS, MATTHEW
 *       korean_name  VARCHAR(30)  NOT NULL,
 *       english_name VARCHAR(50)  NOT NULL,
 *       display_order SMALLINT   NOT NULL UNIQUE      -- 정렬 순서
 *   );
 */
// TODO: @Entity, @Table(name = "bible_books")
public class BibleBook {

    // TODO: @Id
    //        Short id;                   — 1~66 고정 PK (AUTO_INCREMENT 아님)

    // TODO: @Column(nullable = false, length = 10)
    //        String testament;           — "OLD"(구약) / "NEW"(신약)

    // TODO: @Column(nullable = false, length = 20, unique = true)
    //        String code;               — 예: GENESIS, MATTHEW (ERD §2.3)

    // TODO: @Column(name = "korean_name", nullable = false, length = 30)
    //        String koreanName;          — 한글 권명 (예: 창세기)

    // TODO: @Column(name = "english_name", nullable = false, length = 50)
    //        String englishName;         — 영문 권명 (예: Genesis)

    // TODO: @Column(name = "display_order", nullable = false, unique = true)
    //        Short displayOrder;         — 정렬 순서

    // 연관: BibleVerse
    // TODO: @OneToMany(mappedBy = "bookId", fetch = FetchType.LAZY)
    //        List<BibleVerse> verses;
}
