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
    void rejectsBrokenOrAmbiguousHeading() {
        assertThat(parser.parse("19: 1 1-15")).isEmpty();
        assertThat(parser.parse("19:11-15-16")).isEmpty();
        assertThat(parser.parse("chapter 19 verse 11")).isEmpty();
    }
}
