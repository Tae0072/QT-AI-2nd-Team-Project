package com.qtai.domain.qt.internal;

import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;

final class TodayQtRangeMapper {

    private TodayQtRangeMapper() {
    }

    /**
     * 권 메타(bible)와 QT 본문(qt)을 합쳐 Flutter 연동용 범위 응답을 만든다(리뷰 §5.2 #1).
     * 권 메타는 bible api({@link BibleBookResponse})에서, 장/절은 qt 본문({@link QtPassage})에서 가져온다.
     * 장 교차 범위면 displayText를 "9:1-10:5" 형태로 만든다.
     */
    static TodayQtRangeResponse toResponse(BibleBookResponse book, QtPassage passage) {
        Integer chapter = toInteger(passage.getChapter());
        Integer endChapter = toInteger(passage.getEndChapter());
        Integer verseFrom = toInteger(passage.getStartVerse());
        Integer verseTo = toInteger(passage.getEndVerse());
        String displayText = book.koreanName() + " " + chapter + ":" + verseFrom + "-"
                + (chapter != null && chapter.equals(endChapter) ? "" : endChapter + ":")
                + verseTo;
        return new TodayQtRangeResponse(
                book.testament(),
                book.code(),
                book.koreanName(),
                book.englishName(),
                chapter,
                endChapter,
                verseFrom,
                verseTo,
                displayText
        );
    }

    private static Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }
}
