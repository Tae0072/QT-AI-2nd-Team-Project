package com.qtai.domain.mission.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원 미션 진행률 응답 DTO.
 *
 * <p>마이페이지 대시보드(GET /api/v1/me/dashboard)의 {@code missionProgress} 항목으로 노출된다.
 * mission 도메인이 me/dashboard 도메인에 제공하는 도메인 간 계약(DTO)이다.
 *
 * @param missionDefinitionId 미션 정의 ID
 * @param code                미션 코드
 * @param title               미션명
 * @param metricType          달성 지표 타입
 * @param periodType          집계 주기
 * @param currentCount        현재 달성 수치
 * @param targetCount         목표 수치(집계 당시 스냅샷)
 * @param progressRate        진행률 0.00~100.00
 * @param completed           달성 여부
 * @param periodStartDate     집계 시작일
 * @param periodEndDate       집계 종료일
 * @param completedAt         달성 시각 (미달성 시 null)
 */
public record MissionProgressResponse(
        Long missionDefinitionId,
        String code,
        String title,
        String metricType,
        String periodType,
        Integer currentCount,
        Integer targetCount,
        BigDecimal progressRate,
        boolean completed,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        LocalDateTime completedAt) {
}
