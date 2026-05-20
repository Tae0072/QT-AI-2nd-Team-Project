package com.qtai.domain.note.internal;

/**
 * 노트 엔티티.
 *
 * QT에 종속(qtId FK). qt 삭제 시 노트도 함께 삭제될지(cascade) 또는 보존할지 정책 결정 필요.
 */
// TODO: @Entity, @Table(name = "note")
public class Note {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long qtId;                — 종속 QT FK
    // TODO: Long memberId;            — 작성자 FK
    // TODO: @Column(columnDefinition="TEXT") String content;
    // TODO: LocalDateTime createdAt;  — @CreationTimestamp
    // TODO: LocalDateTime updatedAt;  — @UpdateTimestamp
}
