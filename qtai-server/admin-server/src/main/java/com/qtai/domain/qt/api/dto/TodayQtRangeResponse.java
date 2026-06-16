package com.qtai.domain.qt.api.dto;

/**
 * Today QT 본문 범위 응답.
 *
 * <p>{@code chapter}는 시작 장, {@code endChapter}는 종료 장이다. 같은 장이면 둘이 같다.
 * 장 교차 범위(예: 9:1-10:5)도 표현하기 위해 종료 장을 함께 내려준다.
 */
public record TodayQtRangeResponse(
        String testament,
        String bookCode,
        String koreanBookName,
        String englishBookName,
        Integer chapter,
        Integer endChapter,
        Integer verseFrom,
        Integer verseTo,
        String displayText
) {
}
