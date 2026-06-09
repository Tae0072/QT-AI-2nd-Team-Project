package com.qtai.domain.ai.internal;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AiReviewReferencePdfHeadingParser {

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(?<startChapter>\\d{1,3})\\s*:\\s*(?<startVerse>\\d{1,3})"
                    + "(?:\\s*-\\s*(?:(?<endChapter>\\d{1,3})\\s*:\\s*)?(?<endVerse>\\d{1,3}))?"
                    + "\\s*$"
    );
    private static final Pattern COMPACT_HEADING_PATTERN = Pattern.compile(
            "^(?<startChapter>\\d{1,3}):(?<startVerse>\\d{1,3})"
                    + "(?:-(?:(?<endChapter>\\d{1,3}):)?(?<endVerse>\\d{1,3}))?"
                    + "$"
    );
    private static final List<String> BOOK_NAME_ALIASES = bookNameAliases();

    Optional<ParsedHeading> parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        String line = rawLine.trim();

        return parseHeading(line)
                .or(() -> parseBookNamePrefixedHeading(line));
    }

    private Optional<ParsedHeading> parseHeading(String line) {
        Matcher matcher = HEADING_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return parsedHeading(matcher);
    }

    private Optional<ParsedHeading> parseBookNamePrefixedHeading(String line) {
        String compactLine = normalizeForBookNameHeading(line);
        Optional<ParsedHeading> rightmostHeading = Optional.empty();
        int rightmostBookNameIndex = -1;

        for (String bookNameAlias : BOOK_NAME_ALIASES) {
            int bookNameIndex = compactLine.indexOf(bookNameAlias);
            while (bookNameIndex >= 0) {
                Optional<ParsedHeading> heading = parseCompactHeading(
                        compactLine.substring(bookNameIndex + bookNameAlias.length())
                );
                if (heading.isPresent() && bookNameIndex >= rightmostBookNameIndex) {
                    rightmostHeading = heading;
                    rightmostBookNameIndex = bookNameIndex;
                }
                bookNameIndex = compactLine.indexOf(bookNameAlias, bookNameIndex + 1);
            }
        }

        return rightmostHeading;
    }

    private Optional<ParsedHeading> parseCompactHeading(String suffix) {
        Matcher matcher = COMPACT_HEADING_PATTERN.matcher(suffix);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return parsedHeading(matcher);
    }

    private static Optional<ParsedHeading> parsedHeading(Matcher matcher) {
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

    private static String normalizeForBookNameHeading(String line) {
        return line
                .replace('’', ':')
                .replace('‘', ':')
                .replace('ʼ', ':')
                .replace('`', ':')
                .replace('：', ':')
                .replace('–', '-')
                .replace('—', '-')
                .replace('‐', '-')
                .replaceAll("\\s+", "");
    }

    private static List<String> bookNameAliases() {
        List<String> catalogBookNames = AiReviewReferenceBookCatalog.books().stream()
                .map(AiReviewReferenceBookCatalog.Book::koreanName)
                .toList();
        return java.util.stream.Stream.concat(
                        catalogBookNames.stream(),
                        List.of("오바다", "나홍", "벌레몬서", "벌레몬 서").stream()
                )
                .map(AiReviewReferencePdfHeadingParser::normalizeForBookNameHeading)
                .distinct()
                .toList();
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
