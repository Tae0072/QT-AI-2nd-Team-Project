package com.qtai.domain.ai.api.admin.evaluation.dto;

/**
 * 신고 → 평가 케이스 후보 등록 커맨드.
 *
 * <p>adminId/memberRole/adminRole은 인증 주체에서 채워지고, FE는 evaluationSetId·expectedPolicyJson(선택)만 보낸다.
 * inputJson은 백엔드가 reportId 기준 메타데이터로 조립한다.
 */
public record CreateAiEvaluationReportCandidateCommand(
        Long adminId,
        String memberRole,
        String adminRole,
        Long evaluationSetId,
        Long reportId,
        String expectedPolicyJson
) {
}
