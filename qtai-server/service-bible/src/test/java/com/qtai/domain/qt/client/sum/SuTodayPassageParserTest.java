package com.qtai.domain.qt.client.sum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuTodayPassageParserTest {

    private final SuTodayPassageParser parser = new SuTodayPassageParser();

    @Test
    @DisplayName("성서유니온 오늘 본문 HTML에서 제목과 권장절 범위를 파싱한다(같은 장)")
    void parseToday_extractsTitleAndReferenceRange() {
        String html = """
                <div class="bible_text" id="bible_text">같은 말, 같은 마음, 같은 뜻</div>
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 고린도전서(1 Corinthians) 1:10 - 1:17 찬송가 455장
                </div>
                """;

        SuTodayPassage result = parser.parseToday(html);

        assertThat(result.title()).isEqualTo(result.referenceText());
        assertThat(result.koreanBookName()).isEqualTo("고린도전서");
        assertThat(result.englishBookName()).isEqualTo("1 Corinthians");
        assertThat(result.chapter()).isEqualTo((short) 1);
        assertThat(result.endChapter()).isEqualTo((short) 1);
        assertThat(result.startVerse()).isEqualTo((short) 10);
        assertThat(result.endVerse()).isEqualTo((short) 17);
        assertThat(result.referenceText()).isEqualTo("고린도전서(1 Corinthians) 1:10-17");
    }

    @Test
    @DisplayName("성서유니온 범위가 장을 넘기면 시작/종료 장을 함께 파싱한다")
    void parseToday_parsesCrossChapterRange() {
        String html = """
                <div class="bible_text" id="bible_text">제목</div>
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 요한복음(John) 3:36 - 4:2 찬송가 455장
                </div>
                """;

        SuTodayPassage result = parser.parseToday(html);

        assertThat(result.koreanBookName()).isEqualTo("요한복음");
        assertThat(result.englishBookName()).isEqualTo("John");
        assertThat(result.chapter()).isEqualTo((short) 3);
        assertThat(result.endChapter()).isEqualTo((short) 4);
        assertThat(result.startVerse()).isEqualTo((short) 36);
        assertThat(result.endVerse()).isEqualTo((short) 2);
        assertThat(result.referenceText()).isEqualTo("요한복음(John) 3:36-4:2");
    }

    @Test
    @DisplayName("종료 장이 시작 장보다 앞서면 거부한다")
    void parseToday_rejectsDescendingChapterRange() {
        String html = """
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 요한복음(John) 4:2 - 3:36 찬송가 455장
                </div>
                """;

        assertThatThrownBy(() -> parser.parseToday(html))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("종료 장")
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
