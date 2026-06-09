package com.qtai.domain.note.client.bible;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bible 도메인 {@link GetBibleVerseUseCase}의 service-note 임시 구현(Mock).
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): bible은 service-bible 소관이라 service-note에서는 api 계약 타입만
 * 가져와 client 어댑터로 임시 구현한다. 통합 시 이 Mock을 RestClient 호출 어댑터로 교체한다.
 *
 * <p>저작권 정책(CLAUDE.md §8): 본문 텍스트(koreanText/englishText)는 채우지 않는다(null). note는
 * 구절 메타(id·book·chapter·verse)만 사용하므로 본문 없이도 동작한다.
 */
@Component("noteBibleVerseUseCaseMock")
public class GetBibleVerseUseCaseMock implements GetBibleVerseUseCase {

    @Override
    public BibleVerseResponse getVerse(Long verseId) {
        return synthesize(verseId);
    }

    @Override
    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        // note는 요청 id마다 하나의 응답을 기대한다(개수 불일치 시 BIBLE_VERSE_NOT_FOUND).
        // 통합 전까지는 요청 id를 그대로 반영한 합성 메타를 돌려준다.
        return verseIds.stream().map(this::synthesize).toList();
    }

    @Override
    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return new BibleVerseRangeResponse(
                new BibleVerseBookResponse(bookCode, null, null, chapter),
                List.of());
    }

    private BibleVerseResponse synthesize(Long verseId) {
        return new BibleVerseResponse(verseId, "GEN", 1, verseId == null ? 1 : verseId.intValue(), null, null);
    }
}
