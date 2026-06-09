package com.qtai.domain.ai.client.bible;

import java.util.List;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import org.springframework.stereotype.Component;

/**
 * bible 도메인 {@link GetBibleVerseUseCase}의 service-ai 임시 구현(Mock).
 *
 * <p>성경 본문 조회는 bible(콘텐츠) 서비스 소관이라, 미통합 구간에서는 빈 본문/빈 범위를 돌려준다.
 * 이 흐름(해설 생성 job 처리)은 {@code ai.scheduling.enabled=true}일 때만 동작하며 테스트·기동
 * 검증에서는 비활성이다. 통합 시 RestClient 어댑터로 교체하고 이 Mock은 삭제한다(CLAUDE.md §4).
 */
@Component("aiGetBibleVerseUseCaseMock")
public class GetBibleVerseUseCaseMock implements GetBibleVerseUseCase {

    @Override
    public BibleVerseResponse getVerse(Long verseId) {
        return new BibleVerseResponse(verseId, null, null, null, null, null);
    }

    @Override
    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        return List.of();
    }

    @Override
    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return new BibleVerseRangeResponse(null, List.of());
    }
}
