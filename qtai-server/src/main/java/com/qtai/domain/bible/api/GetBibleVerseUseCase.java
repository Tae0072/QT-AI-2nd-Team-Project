package com.qtai.domain.bible.api;

import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import java.util.List;

public interface GetBibleVerseUseCase {

    BibleVerseResponse getVerse(Long verseId);

    List<BibleVerseResponse> getVerses(List<Long> verseIds);

    BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo);
}
