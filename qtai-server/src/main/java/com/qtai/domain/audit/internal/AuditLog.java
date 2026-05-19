package com.qtai.domain.audit.internal;

/**
 * 감사 로그 엔티티.
 *
 * append-only 정책: INSERT만 허용, UPDATE/DELETE 금지 —
 * 누구라도 사후에 로그를 조작할 수 없도록 한다(법적 증빙·사고 추적용).
 */
// TODO: @Entity, @Table(name = "audit_log")
public class AuditLog {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long actorId;             — 행위자 (nullable, 시스템 작업 표현)
    // TODO: String action;            — 액션 코드 (도메인_동사)
    // TODO: String targetType;
    // TODO: Long targetId;
    // TODO: @Column(columnDefinition="TEXT") String metadata;  — JSON 문자열
    // TODO: LocalDateTime createdAt;  — @CreationTimestamp
}
