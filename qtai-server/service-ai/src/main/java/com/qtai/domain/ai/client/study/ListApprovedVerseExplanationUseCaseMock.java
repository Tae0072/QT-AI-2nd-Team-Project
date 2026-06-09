package com.qtai.domain.ai.client.study;

import java.util.List;

import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import org.springframework.stereotype.Component;

/**
 * study 도메인 {@link ListApprovedVerseExplanationUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>승인된 해설 조회는 study(콘텐츠) 서비스 소관이라, 미통합 구간에서는 빈 목록을 돌려준다.
 * 통합 시 RestClient 어댑터로 교체하고 이 Mock은 삭제한다(CLAUDE.md §4).
 */
@Component("aiListApprovedVerseExplanationUseCaseMock")
public class ListApprovedVerseExplanationUseCaseMock implements ListApprovedVerseExplanationUseCase {

    @Override
    public List<ApprovedVerseExplanationResponse> listApprovedByVerseIds(List<Long> verseIds) {
        return List.of();
    }
}
