package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiReviewReferencePdfHeadingParserTest {

    private final AiReviewReferencePdfHeadingParser parser = new AiReviewReferencePdfHeadingParser();

    @Test
    void parsesSingleVerseHeading() {
        AiReviewReferencePdfHeadingParser.ParsedHeading heading = parser.parse("19:8").orElseThrow();

        assertThat(heading.detectedHeading()).isEqualTo("19:8");
        assertThat(heading.chapterStart()).isEqualTo(19);
        assertThat(heading.verseStart()).isEqualTo(8);
        assertThat(heading.chapterEnd()).isEqualTo(19);
        assertThat(heading.verseEnd()).isEqualTo(8);
        assertThat(heading.referenceRangeLabel()).isEqualTo("19:8");
    }

    @Test
    void parsesSameChapterVerseRange() {
        AiReviewReferencePdfHeadingParser.ParsedHeading heading = parser.parse("19:11-15").orElseThrow();

        assertThat(heading.chapterStart()).isEqualTo(19);
        assertThat(heading.verseStart()).isEqualTo(11);
        assertThat(heading.chapterEnd()).isEqualTo(19);
        assertThat(heading.verseEnd()).isEqualTo(15);
    }

    @Test
    void parsesLongSameChapterVerseRange() {
        AiReviewReferencePdfHeadingParser.ParsedHeading heading = parser.parse("20:1-26").orElseThrow();

        assertThat(heading.chapterStart()).isEqualTo(20);
        assertThat(heading.verseStart()).isEqualTo(1);
        assertThat(heading.chapterEnd()).isEqualTo(20);
        assertThat(heading.verseEnd()).isEqualTo(26);
    }

    @Test
    void parsesCrossChapterVerseRange() {
        AiReviewReferencePdfHeadingParser.ParsedHeading heading = parser.parse("19:42-20:6").orElseThrow();

        assertThat(heading.chapterStart()).isEqualTo(19);
        assertThat(heading.verseStart()).isEqualTo(42);
        assertThat(heading.chapterEnd()).isEqualTo(20);
        assertThat(heading.verseEnd()).isEqualTo(6);
    }

    @Test
    void parsesBookNamePrefixedHeading() {
        AiReviewReferencePdfHeadingParser.ParsedHeading heading = parser.parse("시편 8:3 -9:13").orElseThrow();

        assertThat(heading.detectedHeading()).isEqualTo("8:3-9:13");
        assertThat(heading.chapterStart()).isEqualTo(8);
        assertThat(heading.verseStart()).isEqualTo(3);
        assertThat(heading.chapterEnd()).isEqualTo(9);
        assertThat(heading.verseEnd()).isEqualTo(13);
        assertThat(parser.parse("요한이서 1: 1-13").orElseThrow().detectedHeading()).isEqualTo("1:1-13");
        assertThat(parser.parse("요한삼서 1: 1-14").orElseThrow().detectedHeading()).isEqualTo("1:1-14");
    }

    @Test
    void parsesObservedOcrAliasBookNameHeading() {
        assertThat(parser.parse("오바다 1:1 -7").orElseThrow().detectedHeading()).isEqualTo("1:1-7");
        assertThat(parser.parse("나홍1 :1 -2").orElseThrow().detectedHeading()).isEqualTo("1:1-2");
        assertThat(parser.parse("벌레몬 서 1: 10-21").orElseThrow().detectedHeading()).isEqualTo("1:10-21");
    }

    @Test
    void parsesRightmostBookHeadingWhenTwoBooksShareLine() {
        assertThat(parser.parse("잠언 31 : 16-전도서 1 ’ 2").orElseThrow().detectedHeading()).isEqualTo("1:2");
        assertThat(parser.parse("전도서 12 :3 아가 1 :3").orElseThrow().detectedHeading()).isEqualTo("1:3");
        assertThat(parser.parse("스가랴 14 :20-말라기 1: 12").orElseThrow().detectedHeading()).isEqualTo("1:12");
    }

    @Test
    void rejectsBrokenOrAmbiguousHeading() {
        assertThat(parser.parse("19: 1 1-15")).isEmpty();
        assertThat(parser.parse("19:11-15-16")).isEmpty();
        assertThat(parser.parse("chapter 19 verse 11")).isEmpty();
        assertThat(parser.parse("19:8 this is body text, not a heading")).isEmpty();
        assertThat(parser.parse("오바다 1:1 에돔에 대한 설명")).isEmpty();
    }
}
