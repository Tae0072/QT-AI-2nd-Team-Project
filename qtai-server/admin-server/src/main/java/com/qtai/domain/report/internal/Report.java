package com.qtai.domain.report.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 엔티티.
 *
 * <p>ERD: reports 테이블 (§2.18).
 * <p>대상은 (target_type, target_id) 쌍으로 식별하는 다형 참조 — 나눔글/댓글/AI Q&amp;A/AI 산출물.
 * <p>(reporter_member_id, target_type, target_id) UNIQUE — 같은 사람이 같은 대상을 중복 신고할 수 없다.
 * <p>처리 결과는 status로 추적: RECEIVED → REVIEWING → RESOLVED/REJECTED.
 *
 * <p>도메인 경계: 다른 도메인 Entity를 직접 참조하지 않고 Long FK(reporterMemberId, targetId)만 보관한다.
 */
@Entity
@Table(name = "reports",
        indexes = {
                @Index(name = "idx_reports_target", columnList = "target_type, target_id"),
                @Index(name = "idx_reports_status_created", columnList = "status, created_at"),
                @Index(name = "idx_reports_reporter", columnList = "reporter_member_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reports_reporter_target",
                columnNames = {"reporter_member_id", "target_type", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고자 회원 ID (members.id FK — Long FK, 도메인 경계 준수). */
    @Column(name = "reporter_member_id", nullable = false)
    private Long reporterMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReportTargetType targetType;

    /** 신고 대상 ID (target_type에 따라 의미가 달라지는 다형 참조). */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 신고 사유 코드 (예: SPAM, HATE, SEXUAL, FACT_ERROR, UNSAFE_ADVICE 등). */
    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    /** 상세 사유 (선택). */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    /** 처리 관리자 ID (admin_users.id FK, 처리 전 null). */
    @Column(name = "processed_by_admin_id")
    private Long processedByAdminId;

    /** 처리 시각 (처리 전 null). */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Report(Long reporterMemberId, ReportTargetType targetType, Long targetId,
                  String reason, String detail, LocalDateTime createdAt) {
        this.reporterMemberId = reporterMemberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.RECEIVED;
        this.createdAt = createdAt;
    }

    /** 이미 종결(RESOLVED/REJECTED)된 신고인지. */
    public boolean isClosed() {
        return this.status == ReportStatus.RESOLVED || this.status == ReportStatus.REJECTED;
    }

    /**
     * 관리자 처리 — 상태 전이(RECEIVED/REVIEWING → RESOLVED/REJECTED) + 처리자·시각 기록.
     * 이미 종결된 신고는 재처리할 수 없다(상태 전이 위반).
     */
    public void process(Long adminId, ReportStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.processedByAdminId = adminId;
        this.processedAt = now;
        this.updatedAt = now;
    }
}
