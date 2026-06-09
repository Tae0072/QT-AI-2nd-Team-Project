package com.qtai.domain.ai.client.study;

import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;
import org.springframework.stereotype.Component;

/**
 * study 도메인 {@link HidePublishedVerseExplanationUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>게시 해설 숨김 쓰기는 study(콘텐츠) 서비스 소관이라, 미통합 구간에서는 숨김 0건의 안전한
 * 결과를 돌려준다. 통합 시 RestClient 어댑터로 교체하고 이 Mock은 삭제한다(CLAUDE.md §4).
 */
@Component("aiHidePublishedVerseExplanationUseCaseMock")
public class HidePublishedVerseExplanationUseCaseMock implements HidePublishedVerseExplanationUseCase {

    @Override
    public HidePublishedVerseExplanationResult hidePublishedVerseExplanation(
            HidePublishedVerseExplanationCommand command) {
        return new HidePublishedVerseExplanationResult(null, 0);
    }
}
