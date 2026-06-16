package com.qtai.domain.bible.api;

import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import java.util.List;

public interface GetBibleVerseUseCase {

    BibleVerseResponse getVerse(Long verseId);

    List<BibleVerseResponse> getVerses(List<Long> verseIds);

    /**
     * 한 권·한 장 안의 절을 조회한다. {@code verseFrom}/{@code verseTo} 조합으로 범위를 정한다.
     *
     * <ul>
     *   <li>{@code verseFrom == null} → 해당 장 <b>전체 절</b>을 반환한다({@code verseTo}는 무시).
     *       장 교차 QT 수집({@code QtTodayPassageImportService})이 장별 전체 절을 모으는 데 이 동작에 의존한다.</li>
     *   <li>{@code verseFrom != null, verseTo == null} → {@code verseFrom} 한 절만 반환한다.</li>
     *   <li>{@code verseFrom != null, verseTo != null} → {@code verseFrom..verseTo} 범위를 반환한다.</li>
     * </ul>
     *
     * <p>해당 범위에 절이 하나도 없으면 {@code BIBLE_VERSE_NOT_FOUND}, 권 코드가 없으면 {@code BIBLE_BOOK_NOT_FOUND}
     * 예외를 던진다(빈 목록을 반환하지 않는다). 이 계약(특히 {@code verseFrom == null} = 장 전체)을 바꾸려면
     * 위 의존 호출자를 함께 갱신해야 한다.
     */
    BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo);
}
