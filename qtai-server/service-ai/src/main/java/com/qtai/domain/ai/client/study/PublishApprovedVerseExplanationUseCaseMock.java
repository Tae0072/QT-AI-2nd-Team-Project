package com.qtai.domain.ai.client.study;

import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import org.springframework.stereotype.Component;

/**
 * study 도메인 {@link PublishApprovedVerseExplanationUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>승인된 해설을 study(콘텐츠) 서비스에 게시하는 쓰기는 다른 서비스 소관이라, 미통합 구간에서는
 * 게시 효과 없는 안전한 결과를 돌려준다. 통합 시 RestClient 어댑터로 교체하고 이 Mock은 삭제한다
 * (CLAUDE.md §4). 이 흐름(자산 검수 승인)은 관리자/시스템 경로에서만 호출된다.
 */
@Component("aiPublishApprovedVerseExplanationUseCaseMock")
public class PublishApprovedVerseExplanationUseCaseMock implements PublishApprovedVerseExplanationUseCase {

    @Override
    public PublishApprovedVerseExplanationResult publishApprovedVerseExplanation(
            PublishApprovedVerseExplanationCommand command) {
        return new PublishApprovedVerseExplanationResult(null, null, "MOCK_NOT_INTEGRATED");
    }
}
