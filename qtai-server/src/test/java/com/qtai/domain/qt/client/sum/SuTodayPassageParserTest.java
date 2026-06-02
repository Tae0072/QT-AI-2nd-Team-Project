package com.qtai.domain.qt.client.sum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuTodayPassageParserTest {

    private final SuTodayPassageParser parser = new SuTodayPassageParser();

    @Test
    @DisplayName("성서유니온 오늘 본문 HTML에서 제목과 권장절 범위를 파싱한다")
    void parseToday_extractsTitleAndReferenceRange() {
        String html = """
                <div class="bible_text" id="bible_text">같은 말, 같은 마음, 같은 뜻</div>
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 고린도전서(1 Corinthians) 1:10 - 1:17 찬송가 455장
                </div>
                """;

        SuTodayPassage result = parser.parseToday(html);

        assertThat(result.title()).isEqualTo("같은 말, 같은 마음, 같은 뜻");
        assertThat(result.koreanBookName()).isEqualTo("고린도전서");
        assertThat(result.englishBookName()).isEqualTo("1 Corinthians");
        assertThat(result.chapter()).isEqualTo((short) 1);
        assertThat(result.startVerse()).isEqualTo((short) 10);
        assertThat(result.endVerse()).isEqualTo((short) 17);
        assertThat(result.referenceText()).isEqualTo("고린도전서(1 Corinthians) 1:10-17");
    }

    @Test
    @DisplayName("성서유니온 범위가 다른 장이면 저장하지 않는다")
    void parseToday_rejectsCrossChapterRange() {
        String html = """
                <div class="bible_text" id="bible_text">제목</div>
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 요한복음(John) 3:36 - 4:2 찬송가 455장
                </div>
                """;

        assertThatThrownBy(() -> parser.parseToday(html))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 장");
    }
}
