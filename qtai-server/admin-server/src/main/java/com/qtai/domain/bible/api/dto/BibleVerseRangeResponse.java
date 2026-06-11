package com.qtai.domain.bible.api.dto;

import java.util.List;

public record BibleVerseRangeResponse(
        BibleVerseBookResponse book,
        List<BibleVerseResponse> verses
) {
}
