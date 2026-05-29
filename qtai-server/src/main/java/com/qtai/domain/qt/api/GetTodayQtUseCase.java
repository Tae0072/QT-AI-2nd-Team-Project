package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * 오늘 QT 통합 조회 UseCase 포트.
 *
 * <p>GET /api/v1/qt/today
 *
 * <p>정책 (CLAUDE.md §6):
 * <ul>
 *   <li>Caffeine 캐시 우선 → miss 시 MySQL 조회</li>
 *   <li>00:00 KST 공개, 배치는 04:00 KST</li>
 *   <li>00:00~04:00 사이에는 이전에 준비된 캐시 제공</li>
 * </ul>
 *
 * <p>"Today QT 100%": 본문 + 해설 진입점 + 노트 진입점 + 시뮬레이터 상태 반환.
 * 시뮬레이터 상태는 READY · MISSING · FAILED · DISABLED 중 하나 (CLAUDE.md §6).
 */
public interface GetTodayQtUseCase {

    /**
     * 오늘의 QT 본문 통합 응답을 반환한다.
     *
     * @param memberId 인증된 사용자 ID (DRAFT 노트 조회용, null 허용)
     * @return 오늘의 QT 본문 + 해설 진입점 + 시뮬레이터 상태 + 캐시 상태
     */
    TodayQtResponse getToday(Long memberId);

    /**
     * 특정 QT 본문을 ID로 조회한다.
     *
     * @param memberId      인증된 사용자 ID (DRAFT 노트 조회용, null 허용)
     * @param qtPassageId   QT 본문 식별자
     * @return QT 본문 통합 응답
     */
    TodayQtResponse getPassage(Long memberId, Long qtPassageId);
}
