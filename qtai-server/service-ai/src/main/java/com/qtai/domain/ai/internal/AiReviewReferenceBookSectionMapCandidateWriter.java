package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class AiReviewReferenceBookSectionMapCandidateWriter {

    static final String SCHEMA_VERSION = "ai-review-reference-book-section-map-candidate.v1";

    private final ObjectMapper objectMapper;

    AiReviewReferenceBookSectionMapCandidateWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    void write(CandidateDocument document, Path outputPath, Path summaryPath) {
        try {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            Files.createDirectories(summaryPath.toAbsolutePath().getParent());
            objectMapper.writeValue(outputPath.toFile(), document);
            objectMapper.writeValue(summaryPath.toFile(), SummaryDocument.from(document));
        } catch (IOException exception) {
            throw new IllegalStateException("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_CANDIDATE_WRITE_FAILED", exception);
        }
    }

    enum Confidence {
        HIGH,
        LOW,
        MISSING
    }

    record CandidateDocument(
            String schemaVersion,
            String sourceFileName,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            Summary summary,
            List<CandidateSection> sections
    ) {

        CandidateDocument {
            sections = List.copyOf(sections);
        }

        static CandidateDocument fromSections(
                String sourceFileName,
                String sourceFileHash,
                OffsetDateTime generatedAt,
                List<CandidateSection> sections,
                List<MissingBook> missingBooks,
                int duplicateDetectionCount
        ) {
            return new CandidateDocument(
                    SCHEMA_VERSION,
                    sourceFileName,
                    sourceFileHash,
                    generatedAt,
                    Summary.from(sections, missingBooks, duplicateDetectionCount),
                    sections
            );
        }
    }

    record CandidateSection(
            String bookCode,
            String bookName,
            int pageStart,
            int pageEnd,
            String detectedTitle,
            Confidence confidence,
            List<String> reasons
    ) {

        CandidateSection {
            reasons = List.copyOf(reasons);
        }
    }

    record MissingBook(
            String bookCode,
            String bookName,
            int displayOrder
    ) {
    }

    record Summary(
            int totalBookCount,
            int detectedSectionCount,
            int highConfidenceSectionCount,
            int lowConfidenceSectionCount,
            int missingBookCount,
            int duplicateDetectionCount,
            List<MissingBook> missingBooks
    ) {

        Summary {
            missingBooks = List.copyOf(missingBooks);
        }

        static Summary from(
                List<CandidateSection> sections,
                List<MissingBook> missingBooks,
                int duplicateDetectionCount
        ) {
            int high = 0;
            int low = 0;
            for (CandidateSection section : sections) {
                if (section.confidence() == Confidence.HIGH) {
                    high++;
                } else if (section.confidence() == Confidence.LOW) {
                    low++;
                }
            }
            return new Summary(
                    AiReviewReferenceBookCatalog.books().size(),
                    sections.size(),
                    high,
                    low,
                    missingBooks.size(),
                    duplicateDetectionCount,
                    missingBooks
            );
        }
    }

    private record SummaryDocument(
            String schemaVersion,
            String sourceFileName,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            Summary summary,
            List<SummarySection> sections
    ) {

        private static SummaryDocument from(CandidateDocument document) {
            return new SummaryDocument(
                    document.schemaVersion(),
                    document.sourceFileName(),
                    document.sourceFileHash(),
                    document.generatedAt(),
                    document.summary(),
                    document.sections().stream()
                            .map(SummarySection::from)
                            .toList()
            );
        }
    }

    private record SummarySection(
            String bookCode,
            String bookName,
            int pageStart,
            int pageEnd,
            String detectedTitle,
            Confidence confidence,
            List<String> reasons
    ) {

        private static SummarySection from(CandidateSection section) {
            return new SummarySection(
                    section.bookCode(),
                    section.bookName(),
                    section.pageStart(),
                    section.pageEnd(),
                    section.detectedTitle(),
                    section.confidence(),
                    section.reasons()
            );
        }
    }
}
