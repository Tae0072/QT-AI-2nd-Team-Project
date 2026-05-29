package com.qtai.domain.mission.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 미션 진행률 엔티티.
 *
 * <p>ERD: member_mission_progress 테이블 (§2.24).
 * <p>(member_id, mission_definition_id, period_start_date) UNIQUE — 기간 단위로 1개 진행 레코드.
 * <p>도메인 경계: 회원/미션정의를 Long FK로만 보관한다.
 * <p>진행률(progress_rate)은 노트 활동 집계 배치가 갱신한다(ERD §2.24 계산 기준). 본 엔티티는
 * 그 결과를 보관·조회하는 읽기 모델이다.
 */
@Entity
@Table(name = "member_mission_progress",
        indexes = @Index(name = "idx_member_mission_member_period",
                columnList = "member_id, period_start_date, period_end_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_member_mission_period",
                columnNames = {"member_id", "mission_definition_id", "period_start_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMissionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "mission_definition_id", nullable = false)
    private Long missionDefinitionId;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "current_count", nullable = false)
    private Integer currentCount;

    @Column(name = "target_count_snapshot", nullable = false)
    private Integer targetCountSnapshot;

    @Column(name = "progress_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressRate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public MemberMissionProgress(Long memberId, Long missionDefinitionId,
                                 LocalDate periodStartDate, LocalDate periodEndDate,
                                 Integer currentCount, Integer targetCountSnapshot,
                                 BigDecimal progressRate, LocalDateTime completedAt,
                                 LocalDateTime lastCalculatedAt, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.missionDefinitionId = missionDefinitionId;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.currentCount = currentCount != null ? currentCount : 0;
        this.targetCountSnapshot = targetCountSnapshot;
        this.progressRate = progressRate != null ? progressRate : BigDecimal.ZERO;
        this.completedAt = completedAt;
        this.lastCalculatedAt = lastCalculatedAt;
        this.createdAt = createdAt;
    }

    /** 달성 여부 — completed_at 존재 여부로 판단. */
    public boolean isCompleted() {
        return completedAt != null;
    }
}
