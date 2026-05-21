package com.qtai.domain.note.internal;

/**
 * 노트 선택 구절 연결 테이블 엔티티 (ERD §2.14 note_verses).
 *
 * 설교 노트(SERMON) 전용: 성경 절 좌표 대신 bible_verses.id FK 참조 방식 사용.
 * ERD §2.14 기준 컬럼: note_id, bible_verse_id, display_order.
 *
 * 설교 노트 생성 시 bible_verse_id 가 최소 1개 이상 필수이며, 누락 시 저장 실패.
 * UNIQUE 제약: (note_id, bible_verse_id) — 같은 절 중복 선택 방지.
 *
 * DDL 예시:
 *   CREATE TABLE note_verses (
 *       id              BIGINT   AUTO_INCREMENT PRIMARY KEY,
 *       note_id         BIGINT   NOT NULL,
 *       bible_verse_id  BIGINT   NOT NULL,   -- bible_verses.id FK
 *       display_order   SMALLINT NOT NULL,
 *       UNIQUE KEY uk_note_verse (note_id, bible_verse_id),
 *       INDEX idx_note_verses_note_order (note_id, display_order),
 *       FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
 *       FOREIGN KEY (bible_verse_id) REFERENCES bible_verses(id)
 *   );
 */
// TODO: @Entity, @Table(name = "note_verses",
//        uniqueConstraints = @UniqueConstraint(name = "uk_note_verse",
//            columnNames = {"note_id", "bible_verse_id"}))
public class NoteVerse {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @ManyToOne(fetch = FetchType.LAZY)
    //        @JoinColumn(name = "note_id", nullable = false)
    //        Note note;

    // TODO: @Column(name = "bible_verse_id", nullable = false)
    //        Long bibleVerseId;          — bible_verses.id FK (ERD §2.14)

    // TODO: @Column(name = "display_order", nullable = false)
    //        Short displayOrder;         — 선택 순서
}
