package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.QtStudyContentResponse;

/**
 * QT 학습 콘텐츠(해설·용어·요약) 조회 UseCase 포트.
 *
 * GET /api/v1/qt/{qtPassageId}/study-content
 *
 * 정책 (CLAUDE.md §6·§7):
 * - 사전 생성·검증 완료된 콘텐츠만 반환 (verse_explanations ACTIVE 기준)
 * - 사용자 요청 경로에서 즉시 생성 금지
 * - 검증용 주석 원문·참조 자료는 사용자 응답에 노출 금지
 * - 승인된 사용자 노출 해설만 포함
 */
public interface GetQtStudyContentUseCase {

    // TODO: QtStudyContentResponse getStudyContent(Long qtPassageId);
}
