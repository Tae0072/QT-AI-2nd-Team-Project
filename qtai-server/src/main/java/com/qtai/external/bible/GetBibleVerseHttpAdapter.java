package com.qtai.external.bible;

import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

/**
 * {@link GetBibleVerseUseCase}의 HTTP 어댑터 (mode=http일 때 {@code @Primary}로 등록).
 *
 * <p>단건 {@link #getVerse(Long)}는 bible-service에 전용 엔드포인트가 없어 배치({@code /verses/by-ids})로
 * 위임한다(요청 ID가 없으면 배치가 404 → {@link BusinessException} 전파). 범위·배치 조회는 대응 엔드포인트로 직접 호출.
 */
public class GetBibleVerseHttpAdapter implements GetBibleVerseUseCase {

    private final BibleServiceClient client;

    public GetBibleVerseHttpAdapter(BibleServiceClient client) {
        this.client = client;
    }

    @Override
    public BibleVerseResponse getVerse(Long verseId) {
        List<BibleVerseResponse> verses = client.getVerses(List.of(verseId));
        if (verses.isEmpty()) {
            throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        }
        return verses.get(0);
    }

    @Override
    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        return client.getVerses(verseIds);
    }

    @Override
    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        return client.getVerses(bookCode, chapter, verseFrom, verseTo);
    }
}
