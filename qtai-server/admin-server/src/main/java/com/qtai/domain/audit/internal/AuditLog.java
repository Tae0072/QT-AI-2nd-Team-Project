package com.qtai.domain.audit.internal;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 감사 로그 엔티티.
 *
 * append-only 정책: INSERT만 허용, UPDATE/DELETE 금지 —
 * 누구라도 사후에 로그를 조작할 수 없도록 한다(법적 증빙·사고 추적용).
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_label", nullable = false, length = 100)
    private String actorLabel;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "before_json", columnDefinition = "LONGTEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "LONGTEXT")
    private String afterJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    private AuditLog(
            Long adminUserId,
            String actorType,
            Long actorId,
            String actorLabel,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson,
            OffsetDateTime createdAt
    ) {
        this.adminUserId = adminUserId;
        this.actorType = requireText(actorType, "actorType");
        this.actorId = actorId;
        this.actorLabel = requireText(actorLabel, "actorLabel");
        this.actionType = requireText(actionType, "actionType");
        this.targetType = requireText(targetType, "targetType");
        this.targetId = targetId;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AuditLog create(
            Long adminUserId,
            String actorType,
            Long actorId,
            String actorLabel,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson,
            OffsetDateTime createdAt
    ) {
        return new AuditLog(
                adminUserId,
                actorType,
                actorId,
                actorLabel,
                actionType,
                targetType,
                targetId,
                beforeJson,
                afterJson,
                createdAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public String getActorType() {
        return actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorLabel() {
        return actorLabel;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
