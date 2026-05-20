package com.qtai.domain.bible.internal;

/**
 * 성경 절 엔티티.
 *
 * 저작권 주의: 사용 번역본 선정 시 라이선스 확인 필수 — 공유 번역(예: KJV, 개역개정 일부)
 * 또는 명시 허가 받은 번역만 시드한다. 위반 시 법적 리스크.
 */
// TODO: @Entity, @Table(name = "bible_verse",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"book","chapter","verse","translation"}))
public class BibleVerse {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: String book;          — 책 이름 (예: "Genesis", "창세기")
    // TODO: Integer chapter;
    // TODO: Integer verse;
    // TODO: @Column(columnDefinition="TEXT") String text;  — 절 본문
    // TODO: String translation;   — 번역본 코드 (예: "NIV", "KRV")
    // TODO: FULLTEXT 인덱스는 DDL 또는 @Index로 설정 (text 컬럼 대상)
}
