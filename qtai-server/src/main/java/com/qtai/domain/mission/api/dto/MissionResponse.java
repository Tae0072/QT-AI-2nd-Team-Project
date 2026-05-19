package com.qtai.domain.mission.api.dto;

/** 미션 응답 DTO. */
public record MissionResponse(
        // TODO: Long id
        // TODO: Long memberId
        // TODO: String type            — DAILY_QT / WEEKLY_BIBLE / READING_PLAN 등
        // TODO: String status          — IN_PROGRESS / COMPLETED / EXPIRED
        // TODO: Integer progress       — 진행률 (0~100) 또는 진행 횟수
        // TODO: Integer target         — 목표 횟수 (예: 7일 미션이면 7)
        // TODO: LocalDateTime startedAt
        // TODO: LocalDateTime completedAt — 완료 시각 (미완료면 null)
) {}
