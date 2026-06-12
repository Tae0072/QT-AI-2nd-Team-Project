package com.qtai.domain.report.api.dto;

/**
 * 평가 케이스 후보 등록(USER_REPORT)용 신고 메타데이터.
 *
 * <p>도메인 경계: AI 도메인이 신고 원문/상세를 직접 보지 않고, 평가 inputJson 조립에 필요한
 * 식별자·메타데이터만 전달받는다(원문/프롬프트/민감정보 미포함, CLAUDE.md §7).
 */
public record ReportForEvaluation(
        Long id,
        String targetType,   // POST / COMMENT / AI_QA_REQUEST / AI_ASSET
        Long targetId,
        String reason,
        String status,       // RECEIVED / REVIEWING / RESOLVED / REJECTED
        Long reporterMemberId
) {
}
