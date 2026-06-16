package com.qtai.domain.ai.api.admin.asset.dto;

/**
 * 관리자 해설 생성 트리거 결과 (F-02/F-06).
 *
 * @param createdCount 새로 생성 요청된 해설 job 수
 * @param failedCount  생성 요청 실패 수
 * @param reason       생성이 0건일 때의 사유 코드(없으면 {@code null}).
 *                     예: {@code ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND}
 */
public record GenerateQtPassageExplanationResult(
        int createdCount,
        int failedCount,
        String reason
) {
}
