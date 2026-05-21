package com.qtai.domain.qt.internal;

/**
 * QT 본문 단위 엔티티 (오늘의 QT 컨텐츠 묶음).
 *
 * 성서유니온 QT 본문 텍스트 자체는 저장하지 않는다 (CLAUDE.md §8 저작권 규칙).
 * 대신 본문 범위(책·장·절 범위), 날짜, 제목만 보관한다.
 * 실제 성경 절 텍스트는 bible 도메인(bible_verses)에서 조회한다.
 *
 * QT 범위 공개 시각: 00:00 KST, 수집 배치: 04:00 KST.
 * 00:00~04:00 사이에는 이전에 준비된 캐시를 제공한다 (CLAUDE.md §6).
 *
 * DDL 예시:
 *   CREATE TABLE qt_passages (
 *       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
 *       passage_date DATE         NOT NULL UNIQUE,   -- 해당 QT 날짜
 *       title        VARCHAR(200) NOT NULL,
 *       description  TEXT         NULL,              -- 간단한 도입 설명 (저작권 무관 범위)
 *       created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *       updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 *   );
 */
// TODO: @Entity, @Table(name = "qt_passages")
public class QtPassage {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @Column(nullable = false, unique = true)
    //        LocalDate passageDate;      — 해당 QT 날짜

    // TODO: @Column(nullable = false, length = 200)
    //        String title;               — QT 제목

    // TODO: @Column(columnDefinition = "TEXT")
    //        String description;         — 도입 설명 (nullable)

    // TODO: @CreationTimestamp LocalDateTime createdAt;
    // TODO: @UpdateTimestamp  LocalDateTime updatedAt;

    // 연관: QtPassageVerse (본문에 딸린 절 목록)
    // TODO: @OneToMany(mappedBy = "qtPassage", cascade = CascadeType.ALL, orphanRemoval = true)
    //        List<QtPassageVerse> verses;
}
