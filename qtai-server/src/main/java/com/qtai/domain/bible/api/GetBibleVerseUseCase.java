package com.qtai.domain.bible.api;

import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

public interface GetBibleVerseUseCase {

    BibleVerseResponse getVerse(Long verseId);

    BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo);
}
