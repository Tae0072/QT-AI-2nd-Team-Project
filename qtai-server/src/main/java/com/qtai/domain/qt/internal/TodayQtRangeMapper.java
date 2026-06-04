package com.qtai.domain.qt.internal;

import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;

final class TodayQtRangeMapper {

    private TodayQtRangeMapper() {
    }

    static TodayQtRangeResponse toResponse(QtPassageRangeView view) {
        Integer chapter = toInteger(view.getChapter());
        Integer verseFrom = toInteger(view.getVerseFrom());
        Integer verseTo = toInteger(view.getVerseTo());
        String displayText = view.getKoreanBookName() + " " + chapter + ":" + verseFrom + "-" + verseTo;
        return new TodayQtRangeResponse(
                view.getTestament(),
                view.getBookCode(),
                view.getKoreanBookName(),
                view.getEnglishBookName(),
                chapter,
                verseFrom,
                verseTo,
                displayText
        );
    }

    private static Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }
}
