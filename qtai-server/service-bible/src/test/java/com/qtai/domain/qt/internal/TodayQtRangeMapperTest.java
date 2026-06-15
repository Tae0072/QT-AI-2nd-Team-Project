package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TodayQtRangeMapperTest {

    @Test
    @DisplayName("장 교차 범위는 종료 장을 포함한 displayText로 변환한다")
    void toResponse_formatsCrossChapterDisplayText() {
        BibleBookResponse book = new BibleBookResponse(
                46, "NT", "1CO", "고린도전서", "1 Corinthians", 46);
        QtPassage passage = QtPassage.create(
                LocalDate.of(2026, 6, 15),
                (short) 46,
                (short) 46,
                (short) 9,
                (short) 10,
                (short) 20,
                (short) 5,
                "오늘의 QT",
                "고린도전서 9:20-10:5"
        );

        TodayQtRangeResponse response = TodayQtRangeMapper.toResponse(book, passage);

        assertThat(response.chapter()).isEqualTo(9);
        assertThat(response.endChapter()).isEqualTo(10);
        assertThat(response.displayText()).isEqualTo("고린도전서 9:20-10:5");
    }
}
