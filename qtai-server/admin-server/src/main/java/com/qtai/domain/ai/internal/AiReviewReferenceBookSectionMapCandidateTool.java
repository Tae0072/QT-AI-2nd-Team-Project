package com.qtai.domain.ai.internal;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class AiReviewReferenceBookSectionMapCandidateTool {

    private AiReviewReferenceBookSectionMapCandidateTool() {
    }

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        AiReviewReferenceBookSectionMapCandidateGenerator generator =
                new AiReviewReferenceBookSectionMapCandidateGenerator(
                        new AiReviewReferenceBookSectionTitleDetector()
                );
        AiReviewReferenceBookSectionMapCandidateWriter writer =
                new AiReviewReferenceBookSectionMapCandidateWriter(new ObjectMapper());

        AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument document =
                generator.generate(arguments.source());
        writer.write(document, arguments.output(), arguments.summary());

        System.out.printf(
                "AI review reference book section map candidate completed. detected=%d high=%d low=%d missing=%d duplicates=%d%n",
                document.summary().detectedSectionCount(),
                document.summary().highConfidenceSectionCount(),
                document.summary().lowConfidenceSectionCount(),
                document.summary().missingBookCount(),
                document.summary().duplicateDetectionCount()
        );
        System.out.printf("candidate=%s%nsummary=%s%n", arguments.output(), arguments.summary());
    }

    record Arguments(
            Path source,
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

            String source = values.get("source");
            String output = values.get("output");
            String summary = values.get("summary");
            if (source == null || source.isBlank()
                    || output == null || output.isBlank()
                    || summary == null || summary.isBlank()) {
                throw usage();
            }
            Path outputPath = Path.of(output);
            Path summaryPath = Path.of(summary);
            requireBuildOutput(outputPath);
            requireBuildOutput(summaryPath);
            return new Arguments(Path.of(source), outputPath, summaryPath);
        }

        private static IllegalArgumentException usage() {
            return new IllegalArgumentException(
                    "Usage: --source <pdf> --output <book-section-map-candidate-json> --summary <summary-json>"
            );
        }

        private static void requireBuildOutput(Path outputPath) {
            Path buildRoot = Path.of("build").toAbsolutePath().normalize();
            Path normalizedOutput = outputPath.toAbsolutePath().normalize();
            if (!normalizedOutput.startsWith(buildRoot)) {
                throw new IllegalArgumentException(
                        "AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_OUTPUT_MUST_BE_UNDER_BUILD"
                );
            }
        }
    }
}
