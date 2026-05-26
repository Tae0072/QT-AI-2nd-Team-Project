package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.QtSimulatorResponse;

/**
 * QT 시뮬레이터 조회 UseCase 포트.
 *
 * GET /api/v1/qt/{qtPassageId}/simulator
 *
 * 정책 (CLAUDE.md §6):
 * - APPROVED 콘텐츠만 반환
 * - 시뮬레이터 상태: READY · MISSING · FAILED · DISABLED
 * - 버튼은 READY 일 때만 활성화 (Flutter 에서 status 기반으로 판단)
 * - 모든 본문에 실제 시뮬레이터 clip 이 있다는 뜻이 아님
 * - 해설/시뮬레이터 콘텐츠는 batch 또는 admin trigger 로 사전 생성
 * - 사용자 요청 경로에서 즉시 생성 금지
 */
public interface GetQtSimulatorUseCase {

    // TODO: QtSimulatorResponse getSimulator(Long qtPassageId);
}
