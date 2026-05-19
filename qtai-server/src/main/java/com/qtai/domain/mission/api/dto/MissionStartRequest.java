package com.qtai.domain.mission.api.dto;

/** 미션 시작 요청 DTO. */
public record MissionStartRequest(
        // TODO: String type        — 시작할 미션 종류 (DAILY_QT 등)
        // TODO: Integer target     — 목표값 (기간/횟수)
) {}
