package com.qtai.domain.qt.api.dto;

/**
 * QT 시뮬레이터 상태 응답 DTO.
 *
 * GET /api/v1/qt/{qtPassageId}/simulator
 *
 * status 값:
 *   READY    — 시뮬레이터 사용 가능 (버튼 활성화)
 *   MISSING  — 콘텐츠 없음
 *   FAILED   — 생성/검증 실패
 *   DISABLED — 운영자 비활성화
 *
 * Flutter 는 status=READY 일 때만 버튼을 활성화한다.
 */
public record QtSimulatorResponse(
    // TODO: Long qtPassageId;
    // TODO: String status;    — READY / MISSING / FAILED / DISABLED
) {}
