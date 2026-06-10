package com.qtai.domain.ai.internal;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class AiReviewReferenceCandidatePromotionTool {

    private AiReviewReferenceCandidatePromotionTool() {
    }

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        ObjectMapper objectMapper = new ObjectMapper();
        AiReviewReferenceCandidatePromotionService service =
                new AiReviewReferenceCandidatePromotionService(objectMapper);
        String sourceFileHash = AiReviewReferenceCandidatePromotionSupport.sourceFileHash(arguments.candidate());
        AiReviewReferenceBookSectionMapReader.BookSectionMap sectionMap =
                new AiReviewReferenceBookSectionMapReader(objectMapper)
                        .read(arguments.bookSectionMap(), sourceFileHash);

        AiReviewReferenceCandidatePromotionService.PromotionResult result =
                service.promote(arguments.candidate(), sectionMap);
        service.write(result, arguments.output(), arguments.summary());

        System.out.printf(
                "AI review reference candidate promotion completed. candidate=%d promoted=%d unusable=%d unmapped=%d%n",
                result.summary().candidateEntryCount(),
                result.summary().promotedEntryCount(),
                result.summary().unusableEntryCount(),
                result.summary().unmappedEntryCount()
        );
    }

    record Arguments(
            Path candidate,
            Path bookSectionMap,
            Path output,
            Path summary
    ) {

        static Arguments parse(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int index = 0; index < args.length; index++) {
                String key = args[index];
                if (!key.startsWith("--") || index + 1 >= args.length) {
                    throw usage();
                }
                values.put(key.substring(2), args[++index]);
            }

            String candidate = values.get("candidate");
            String bookSectionMap = values.get("book-section-map");
            String output = values.get("output");
            String summary = values.get("summary");
            if (isBlank(candidate) || isBlank(bookSectionMap) || isBlank(output) || isBlank(summary)) {
                throw usage();
            }

            Path outputPath = Path.of(output);
            Path summaryPath = Path.of(summary);
            requireBuildOutput(outputPath);
            requireBuildOutput(summaryPath);
            return new Arguments(Path.of(candidate), Path.of(bookSectionMap), outputPath, summaryPath);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private static void requireBuildOutput(Path outputPath) {
            Path buildRoot = Path.of("build").toAbsolutePath().normalize();
            Path normalizedOutput = outputPath.toAbsolutePath().normalize();
            if (!normalizedOutput.startsWith(buildRoot)) {
                throw new IllegalArgumentException(
                        "AI_REVIEW_REFERENCE_PROMOTION_OUTPUT_MUST_BE_UNDER_BUILD"
                );
            }
        }

        private static IllegalArgumentException usage() {
            return new IllegalArgumentException(
                    "Usage: --candidate <candidate-json> --book-section-map <map-json> "
                            + "--output <reference-index-json> --summary <summary-json>"
            );
        }
    }
}
