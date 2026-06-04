package com.qtai.domain.ai.internal;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AiReviewReferencePdfHeadingParser {

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(?<startChapter>\\d{1,3})\\s*:\\s*(?<startVerse>\\d{1,3})"
                    + "(?:\\s*-\\s*(?:(?<endChapter>\\d{1,3})\\s*:\\s*)?(?<endVerse>\\d{1,3}))?"
                    + "\\s*$"
    );

    Optional<ParsedHeading> parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        String line = rawLine.trim();
        Matcher matcher = HEADING_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int chapterStart = parseInt(matcher.group("startChapter"));
        int verseStart = parseInt(matcher.group("startVerse"));
        int chapterEnd = matcher.group("endChapter") == null
                ? chapterStart
                : parseInt(matcher.group("endChapter"));
        int verseEnd = matcher.group("endVerse") == null
                ? verseStart
                : parseInt(matcher.group("endVerse"));

        if (compare(chapterStart, verseStart, chapterEnd, verseEnd) > 0) {
            return Optional.empty();
        }

        String detectedHeading = normalizedHeading(chapterStart, verseStart, chapterEnd, verseEnd);
        return Optional.of(new ParsedHeading(
                detectedHeading,
                chapterStart,
                verseStart,
                chapterEnd,
                verseEnd,
                detectedHeading
        ));
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private static int compare(int leftChapter, int leftVerse, int rightChapter, int rightVerse) {
        int chapterCompare = Integer.compare(leftChapter, rightChapter);
        if (chapterCompare != 0) {
            return chapterCompare;
        }
        return Integer.compare(leftVerse, rightVerse);
    }

    private static String normalizedHeading(
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd
    ) {
        if (chapterStart == chapterEnd && verseStart == verseEnd) {
            return chapterStart + ":" + verseStart;
        }
        if (chapterStart == chapterEnd) {
            return chapterStart + ":" + verseStart + "-" + verseEnd;
        }
        return chapterStart + ":" + verseStart + "-" + chapterEnd + ":" + verseEnd;
    }

    record ParsedHeading(
            String detectedHeading,
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd,
            String referenceRangeLabel
    ) {
    }
}
