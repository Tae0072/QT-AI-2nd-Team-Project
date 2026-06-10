package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

class AiReviewReferenceBookSectionMapCandidateGenerator {

    private static final int MAX_BOOK_TITLE_DETECTIONS_PER_SECTION_PAGE = 3;

    private final AiReviewReferenceBookSectionTitleDetector titleDetector;

    AiReviewReferenceBookSectionMapCandidateGenerator(AiReviewReferenceBookSectionTitleDetector titleDetector) {
        this.titleDetector = titleDetector;
    }

    AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument generate(Path sourcePath) {
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

        return documentFromPages(
                sourcePath.getFileName().toString(),
                AiReviewReferencePdfIndexCandidateWriter.sha256Hex(sourcePath),
                OffsetDateTime.now(ZoneOffset.UTC),
                pages
        );
    }

    AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument documentFromPages(
            String sourceFileName,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            List<PageText> pages
    ) {
        List<DetectedPageTitle> detectedTitles = detectTitles(pages);
        List<DetectedPageTitle> firstDetectedTitles = firstDetectedTitles(detectedTitles);
        List<AiReviewReferenceBookSectionMapCandidateWriter.CandidateSection> sections =
                sectionsFrom(firstDetectedTitles, pageCount(pages));
        List<AiReviewReferenceBookSectionMapCandidateWriter.MissingBook> missingBooks =
                missingBooks(firstDetectedTitles);

        return AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument.fromSections(
                sourceFileName,
                sourceFileHash,
                generatedAt,
                sections,
                missingBooks,
                detectedTitles.size() - firstDetectedTitles.size()
        );
    }

    private List<DetectedPageTitle> detectTitles(List<PageText> pages) {
        List<DetectedPageTitle> detectedTitles = new ArrayList<>();
        for (PageText page : pages) {
            List<DetectedPageTitle> pageDetectedTitles = new ArrayList<>();
            String[] lines = page.text() == null ? new String[0] : page.text().split("\\R");
            for (String line : lines) {
                Optional<AiReviewReferenceBookSectionTitleDetector.DetectedTitle> detectedTitle =
                        titleDetector.detect(line);
                detectedTitle.ifPresent(title -> pageDetectedTitles.add(new DetectedPageTitle(page.pageNumber(), title)));
            }
            if (pageDetectedTitles.size() <= MAX_BOOK_TITLE_DETECTIONS_PER_SECTION_PAGE) {
                detectedTitles.addAll(pageDetectedTitles);
            }
        }
        return detectedTitles;
    }

    private static List<DetectedPageTitle> firstDetectedTitles(List<DetectedPageTitle> detectedTitles) {
        Map<String, DetectedPageTitle> firstByBookCode = new LinkedHashMap<>();
        for (DetectedPageTitle detectedTitle : detectedTitles) {
            firstByBookCode.putIfAbsent(detectedTitle.title().book().code(), detectedTitle);
        }
        return firstByBookCode.values().stream()
                .sorted(Comparator.comparingInt(DetectedPageTitle::pageNumber)
                        .thenComparingInt(title -> title.title().book().displayOrder()))
                .toList();
    }

    private static List<AiReviewReferenceBookSectionMapCandidateWriter.CandidateSection> sectionsFrom(
            List<DetectedPageTitle> detectedTitles,
            int pageCount
    ) {
        List<AiReviewReferenceBookSectionMapCandidateWriter.CandidateSection> sections = new ArrayList<>();
        int previousBookOrder = 0;
        for (int index = 0; index < detectedTitles.size(); index++) {
            DetectedPageTitle detectedTitle = detectedTitles.get(index);
            AiReviewReferenceBookSectionTitleDetector.DetectedTitle title = detectedTitle.title();
            int pageStart = detectedTitle.pageNumber();
            int pageEnd = index + 1 < detectedTitles.size()
                    ? detectedTitles.get(index + 1).pageNumber() - 1
                    : pageCount;
            List<String> reasons = new ArrayList<>(title.reasons());
            AiReviewReferenceBookSectionMapCandidateWriter.Confidence confidence = title.confidence();
            if (title.book().displayOrder() <= previousBookOrder) {
                confidence = AiReviewReferenceBookSectionMapCandidateWriter.Confidence.LOW;
                reasons.add("BOOK_ORDER_OUT_OF_SEQUENCE");
            }
            previousBookOrder = Math.max(previousBookOrder, title.book().displayOrder());

            sections.add(new AiReviewReferenceBookSectionMapCandidateWriter.CandidateSection(
                    title.book().code(),
                    title.book().koreanName(),
                    pageStart,
                    Math.max(pageStart, pageEnd),
                    title.detectedTitle(),
                    confidence,
                    reasons
            ));
        }
        return sections;
    }

    private static List<AiReviewReferenceBookSectionMapCandidateWriter.MissingBook> missingBooks(
            List<DetectedPageTitle> firstDetectedTitles
    ) {
        List<String> detectedBookCodes = firstDetectedTitles.stream()
                .map(detectedTitle -> detectedTitle.title().book().code())
                .toList();
        return AiReviewReferenceBookCatalog.books().stream()
                .filter(book -> !detectedBookCodes.contains(book.code()))
                .map(book -> new AiReviewReferenceBookSectionMapCandidateWriter.MissingBook(
                        book.code(),
                        book.koreanName(),
                        book.displayOrder()
                ))
                .toList();
    }

    private static int pageCount(List<PageText> pages) {
        return pages.stream()
                .mapToInt(PageText::pageNumber)
                .max()
                .orElse(0);
    }

    record PageText(
            int pageNumber,
            String text
    ) {
    }

    private record DetectedPageTitle(
            int pageNumber,
            AiReviewReferenceBookSectionTitleDetector.DetectedTitle title
    ) {
    }
}
