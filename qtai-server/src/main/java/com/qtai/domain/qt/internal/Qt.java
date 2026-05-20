package com.qtai.domain.qt.internal;

/**
 * QT(Quiet Time) 엔티티 — 본 서비스의 핵심 도메인.
 *
 * 인덱스 권장: (member_id, created_at desc) — 내 QT 목록 최신순 조회가 가장 잦다.
 * 작성자(memberId), 가시범위(visibility)는 권한 정책의 기반.
 */
// TODO: @Entity, @Table(name = "qt", indexes = @Index(columnList = "member_id, created_at"))
public class Qt {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long memberId;                          — 작성자 FK
    // TODO: String title;                           — 제목 (nullable)
    // TODO: @Column(columnDefinition="TEXT") String content;  — 본문
    // TODO: Long bibleVerseId;                      — 참조 BibleVerse FK (nullable)
    // TODO: @Enumerated(STRING) QtVisibility visibility;  — 기본 PRIVATE
    // TODO: LocalDateTime createdAt;                — @CreationTimestamp
    // TODO: LocalDateTime updatedAt;                — @UpdateTimestamp
}
