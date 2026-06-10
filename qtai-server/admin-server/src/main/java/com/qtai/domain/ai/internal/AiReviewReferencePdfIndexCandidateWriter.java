package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class AiReviewReferencePdfIndexCandidateWriter {

    static final String SCHEMA_VERSION = "ai-review-reference-index-candidate.v1";

    private final ObjectMapper objectMapper;

    AiReviewReferencePdfIndexCandidateWriter(ObjectMapper objectMapper) {
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
            throw new IllegalStateException("AI_REVIEW_REFERENCE_PDF_INDEX_WRITE_FAILED", exception);
        }
    }

    static String sha256Hex(String value) {
        return "sha256:" + sha256Bytes(value.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(Path path) {
        try {
            return "sha256:" + sha256Bytes(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("AI_REVIEW_REFERENCE_PDF_SOURCE_READ_FAILED", exception);
        }
    }

    private static String sha256Bytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    record CandidateDocument(
            String schemaVersion,
            String sourceFileName,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            QualitySummary qualitySummary,
            List<CandidateEntry> entries
    ) {

        CandidateDocument {
            entries = List.copyOf(entries);
        }

        static CandidateDocument fromEntries(
                String sourceFileName,
                String sourceFileHash,
                OffsetDateTime generatedAt,
                List<CandidateEntry> entries
        ) {
            return new CandidateDocument(
                    SCHEMA_VERSION,
                    sourceFileName,
                    sourceFileHash,
                    generatedAt,
                    QualitySummary.from(entries),
                    entries
            );
        }
    }

    record CandidateEntry(
            int pageStart,
            String detectedHeading,
            String bookCode,
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd,
            String referenceRangeLabel,
            String referenceText,
            String referenceHash,
            AiReviewReferenceTextQualityAnalyzer.QualityResult quality
    ) {
    }

    record QualitySummary(
            int totalEntryCount,
            int usableEntryCount,
            int needsReviewEntryCount,
            int unusableEntryCount,
            double averageHangulRatio,
            double averageSuspiciousMojibakeRatio,
            int totalReplacementCharCount
    ) {

        static QualitySummary from(List<CandidateEntry> entries) {
            int usable = 0;
            int needsReview = 0;
            int unusable = 0;
            double hangulRatioTotal = 0.0;
            double mojibakeRatioTotal = 0.0;
            int replacementChars = 0;

            for (CandidateEntry entry : entries) {
                AiReviewReferenceTextQualityAnalyzer.QualityResult quality = entry.quality();
                if (quality.status() == AiReviewReferenceTextQualityAnalyzer.QualityStatus.USABLE) {
                    usable++;
                } else if (quality.status() == AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW) {
                    needsReview++;
                } else {
                    unusable++;
                }
                hangulRatioTotal += quality.hangulRatio();
                mojibakeRatioTotal += quality.suspiciousMojibakeRatio();
                replacementChars += quality.replacementCharCount();
            }

            int total = entries.size();
            return new QualitySummary(
                    total,
                    usable,
                    needsReview,
                    unusable,
                    total == 0 ? 0.0 : hangulRatioTotal / total,
                    total == 0 ? 0.0 : mojibakeRatioTotal / total,
                    replacementChars
            );
        }
    }

    private record SummaryDocument(
            String schemaVersion,
            String sourceFileName,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            QualitySummary qualitySummary,
            List<SummaryEntry> entries
    ) {

        private static SummaryDocument from(CandidateDocument document) {
            return new SummaryDocument(
                    document.schemaVersion(),
                    document.sourceFileName(),
                    document.sourceFileHash(),
                    document.generatedAt(),
                    document.qualitySummary(),
                    document.entries().stream()
                            .map(SummaryEntry::from)
                            .toList()
            );
        }
    }

    private record SummaryEntry(
            int pageStart,
            String detectedHeading,
            String bookCode,
            String referenceRangeLabel,
            String referenceHash,
            AiReviewReferenceTextQualityAnalyzer.QualityResult quality
    ) {

        private static SummaryEntry from(CandidateEntry entry) {
            return new SummaryEntry(
                    entry.pageStart(),
                    entry.detectedHeading(),
                    entry.bookCode(),
                    entry.referenceRangeLabel(),
                    entry.referenceHash(),
                    entry.quality()
            );
        }
    }
}
