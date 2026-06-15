package com.qtai.domain.study.api.dto;

/**
 * 관리자 시뮬레이터 클립 목록 조회 쿼리 (AD 시뮬레이터 관리, F-06/F-12).
 *
 * @param status      클립 상태 필터(PENDING/APPROVED/REJECTED/HIDDEN). null/blank=전체
 * @param qtPassageId 특정 QT 본문으로 제한. null=전체
 */
public record ListAdminSimulatorClipsQuery(
        String status,
        Long qtPassageId,
        int page,
        int size
) {
}
