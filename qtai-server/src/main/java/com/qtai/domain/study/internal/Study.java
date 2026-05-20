package com.qtai.domain.study.internal;

/**
 * 스터디 엔티티 — 운영자 큐레이션 컨텐츠.
 *
 * 정책: AI가 자동 생성한 스터디 등록 금지(v3.1). ADMIN이 직접 작성한 것만 INSERT.
 * 인용 성경 절은 study_verse 조인 테이블로 N:N 관리 또는 List<Long>로 단순화.
 */
// TODO: @Entity, @Table(name = "study")
public class Study {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: String title;
    // TODO: @Column(columnDefinition="TEXT") String summary;
    // TODO: @Column(columnDefinition="LONGTEXT") String content;
    // TODO: String category;
    // TODO: Long authorId;             — ADMIN
    // TODO: 인용 절: @ElementCollection List<Long> bibleVerseIds; 또는 별도 join entity
    // TODO: LocalDateTime createdAt;   — @CreationTimestamp
    // TODO: LocalDateTime updatedAt;   — @UpdateTimestamp
}
