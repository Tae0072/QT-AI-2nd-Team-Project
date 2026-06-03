package com.qtai.domain.ai.internal;

import java.util.List;
import java.util.Optional;

class AiReviewReferenceBookSectionTitleDetector {

    Optional<DetectedTitle> detect(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String stripped = line.strip();
        String compact = stripped.replaceAll("\\s+", "");

        for (AiReviewReferenceBookCatalog.Book book : AiReviewReferenceBookCatalog.books()) {
            if (compact.equals(book.koreanName())) {
                return Optional.of(new DetectedTitle(
                        book,
                        stripped,
                        AiReviewReferenceBookSectionMapCandidateWriter.Confidence.HIGH,
                        List.of("EXACT_BOOK_TITLE")
                ));
            }
            if (isWeakTitleLine(compact, book.koreanName())) {
                return Optional.of(new DetectedTitle(
                        book,
                        stripped,
                        AiReviewReferenceBookSectionMapCandidateWriter.Confidence.LOW,
                        List.of("WEAK_BOOK_TITLE_LINE")
                ));
            }
        }
        return Optional.empty();
    }

    private static boolean isWeakTitleLine(String compact, String bookName) {
        if (!compact.contains(bookName)) {
            return false;
        }
        int extraLength = compact.length() - bookName.length();
        return extraLength > 0 && extraLength <= 8
                && (compact.startsWith(bookName) || compact.endsWith(bookName));
    }

    record DetectedTitle(
            AiReviewReferenceBookCatalog.Book book,
            String detectedTitle,
            AiReviewReferenceBookSectionMapCandidateWriter.Confidence confidence,
            List<String> reasons
    ) {
    }
}
