package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationReportCandidateCommand;

/**
 * 사용자 신고를 평가 케이스 후보로 등록하는 UseCase 포트.
 *
 * <p>FE는 판단값(평가 세트·기대 정책)만 보내고, 백엔드가 신고 메타데이터로 inputJson을 조립한다
 * (원문/프롬프트/민감정보 미저장, CLAUDE.md §7). sourceType=USER_REPORT, sourceId=reportId.
 */
public interface CreateAiEvaluationReportCandidateUseCase {

    AiEvaluationCaseResponse createReportCandidate(CreateAiEvaluationReportCandidateCommand command);
}
