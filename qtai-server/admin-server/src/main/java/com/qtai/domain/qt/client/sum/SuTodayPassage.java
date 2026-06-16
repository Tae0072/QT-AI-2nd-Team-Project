package com.qtai.domain.qt.client.sum;

/**
 * 성서유니온 오늘 본문 범위.
 *
 * <p>{@code chapter}는 시작 장, {@code endChapter}는 종료 장이다. 같은 장 범위면 둘이 같다.
 * 성서유니온 매일성경은 같은 권 안에서 장을 넘기는 범위(예: 9:1-10:5)를 자주 내보내므로
 * 시작/종료 장을 함께 보존한다. (권 교차는 소스 표기상 발생하지 않는다)
 */
public record SuTodayPassage(
        String title,
        String koreanBookName,
        String englishBookName,
        Short chapter,
        Short endChapter,
        Short startVerse,
        Short endVerse,
        String referenceText
) {
}
