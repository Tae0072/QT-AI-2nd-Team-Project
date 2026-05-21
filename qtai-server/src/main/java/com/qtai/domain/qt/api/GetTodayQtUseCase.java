package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * 오늘 QT 통합 조회 UseCase 포트.
 *
 * GET /api/v1/qt/today
 *
 * 정책 (CLAUDE.md §6):
 * - Redis 캐시 우선 → miss 시 MySQL 조회
 * - 00:00 KST 공개, 배치는 04:00 KST
 * - 00:00~04:00 사이에는 이전에 준비된 캐시 제공
 *
 * "Today QT 100%": 본문 + 해설 진입점 + 노트 진입점 + 시뮬레이터 상태 반환.
 * 시뮬레이터 상태는 READY · MISSING · FAILED · DISABLED 중 하나 (CLAUDE.md §6).
 */
public interface GetTodayQtUseCase {

    // TODO: TodayQtResponse getToday();
}
