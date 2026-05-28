package com.qtai.domain.admin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 액션 감사 로그 엔티티.
 *
 * <p>누가(adminUserId), 언제(actedAt), 무엇을(action), 어디에(targetType, targetId),
 * 왜(reason) 했는지 영구 기록 — 사후 추적/법적 증빙용.
 *
 * <p>INSERT only, 수정/삭제 금지. BaseEntity를 상속하지 않고 독립적인 엔티티로 정의한다
 * (updatedAt, deletedAt 불필요).
 */
@Entity
@Table(name = "admin_action_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 액션 수행 관리자 ID (admin_users.id). */
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    /** 액션 종류: HIDE, DELETE, DISMISS, ROLE_CHANGE 등. */
    @Column(nullable = false, length = 50)
    private String action;

    /** 대상 타입: QT, NOTE, MEMBER, REPORT 등. */
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    /** 대상 엔티티 ID. */
    @Column(name = "target_id")
    private Long targetId;

    /** 처리 사유 (사용자에게도 노출될 수 있음). */
    @Column(length = 500)
    private String reason;

    /** 액션 시각. */
    @Column(name = "acted_at", nullable = false, updatable = false)
    private LocalDateTime actedAt;

    @Builder
    public AdminActionLog(Long adminUserId, String action, String targetType,
                          Long targetId, String reason) {
        this.adminUserId = adminUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.actedAt = LocalDateTime.now();
    }
}
