package com.qtai.domain.admin.internal;

/**
 * 관리자 액션 감사 로그 엔티티.
 *
 * 누가(adminId), 언제(actedAt), 무엇을(action), 어디에(targetType, targetId),
 * 왜(reason) 했는지 영구 기록 — 사후 추적·법적 증빙용. INSERT only, 수정·삭제 금지.
 */
// TODO: @Entity, @Table(name = "admin_action_log")
public class AdminActionLog {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long adminId;          — 액션 수행 관리자 ID
    // TODO: String action;         — HIDE / DELETE / DISMISS / ROLE_CHANGE 등
    // TODO: String targetType;     — QT / NOTE / MEMBER / REPORT ...
    // TODO: Long targetId;         — 대상 엔티티 ID
    // TODO: String reason;         — 처리 사유 (사용자에게도 노출될 수 있음)
    // TODO: LocalDateTime actedAt; — 액션 시각 (@CreationTimestamp)
}
