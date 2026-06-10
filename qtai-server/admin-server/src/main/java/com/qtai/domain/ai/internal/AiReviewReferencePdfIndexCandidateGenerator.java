package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

class AiReviewReferencePdfIndexCandidateGenerator {

    private final AiReviewReferencePdfHeadingParser headingParser;
    private final AiReviewReferenceTextQualityAnalyzer qualityAnalyzer;

    AiReviewReferencePdfIndexCandidateGenerator(
            AiReviewReferencePdfHeadingParser headingParser,
            AiReviewReferenceTextQualityAnalyzer qualityAnalyzer
    ) {
        this.headingParser = headingParser;
        this.qualityAnalyzer = qualityAnalyzer;
    }

    AiReviewReferencePdfIndexCandidateWriter.CandidateDocument generate(Path sourcePath) {
        List<PageText> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(sourcePath.toFile())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                textStripper.setStartPage(page);
                textStripper.setEndPage(page);
                pages.add(new PageText(page, textStripper.getText(document)));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("AI_REVIEW_REFERENCE_PDF_READ_FAILED", exception);
        }
        List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries = entriesFromPages(pages);

        return AiReviewReferencePdfIndexCandidateWriter.CandidateDocument.fromEntries(
                sourcePath.getFileName().toString(),
                AiReviewReferencePdfIndexCandidateWriter.sha256Hex(sourcePath),
                OffsetDateTime.now(ZoneOffset.UTC),
                entries
        );
    }

    List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entriesFromPages(List<PageText> pages) {
        List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries = new ArrayList<>();
        AiReviewReferencePdfHeadingParser.ParsedHeading currentHeading = null;
        StringBuilder currentText = new StringBuilder();
        int currentPageStart = 0;

        for (PageText page : pages) {
            String[] lines = page.text() == null ? new String[0] : page.text().split("\\R");
            for (String line : lines) {
                Optional<AiReviewReferencePdfHeadingParser.ParsedHeading> heading = headingParser.parse(line);
                if (heading.isPresent()) {
                    flushEntry(currentPageStart, currentHeading, currentText, entries);
                    currentHeading = heading.orElseThrow();
                    currentPageStart = page.pageNumber();
                    currentText.setLength(0);
                    continue;
                }
                if (currentHeading != null && !line.isBlank()) {
                    if (!currentText.isEmpty()) {
                        currentText.append(System.lineSeparator());
                    }
                    currentText.append(line.strip());
                }
            }
        }

        flushEntry(currentPageStart, currentHeading, currentText, entries);
        return entries;
    }

    private void flushEntry(
            int page,
            AiReviewReferencePdfHeadingParser.ParsedHeading heading,
            StringBuilder text,
            List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries
    ) {
        if (heading == null) {
            return;
        }

        String referenceText = text.toString().strip();
        AiReviewReferenceTextQualityAnalyzer.QualityResult quality = qualityAnalyzer.analyze(referenceText)
                .withReason("BOOK_CODE_NOT_DETECTED", AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW);
        entries.add(new AiReviewReferencePdfIndexCandidateWriter.CandidateEntry(
                page,
                heading.detectedHeading(),
                null,
                heading.chapterStart(),
                heading.verseStart(),
                heading.chapterEnd(),
                heading.verseEnd(),
                heading.referenceRangeLabel(),
                referenceText,
                AiReviewReferencePdfIndexCandidateWriter.sha256Hex(referenceText),
                quality
        ));
    }

    record PageText(
            int pageNumber,
            String text
    ) {
    }
}
