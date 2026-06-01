package com.qtai.domain.mission.internal;

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
 * 미션 정의 엔티티 (미션 카탈로그).
 *
 * <p>ERD: mission_definitions 테이블 (§2.23).
 * <p>운영자가 등록하는 미션의 정의(코드·지표·주기·목표 수치). 회원별 진행 상태는
 * {@link MemberMissionProgress}가 보관한다(정의와 진행을 분리).
 */
@Entity
@Table(name = "mission_definitions",
        indexes = @Index(name = "idx_mission_definitions_status", columnList = "status"),
        uniqueConstraints = @UniqueConstraint(name = "uk_mission_definitions_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 30)
    private MissionMetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private MissionPeriodType periodType;

    @Column(name = "target_count", nullable = false)
    private Integer targetCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionDefinitionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public MissionDefinition(String code, String title, MissionMetricType metricType,
                             MissionPeriodType periodType, Integer targetCount,
                             MissionDefinitionStatus status, LocalDateTime createdAt) {
        this.code = code;
        this.title = title;
        this.metricType = metricType;
        this.periodType = periodType != null ? periodType : MissionPeriodType.MONTHLY;
        this.targetCount = targetCount;
        this.status = status != null ? status : MissionDefinitionStatus.ACTIVE;
        this.createdAt = createdAt;
    }
}
