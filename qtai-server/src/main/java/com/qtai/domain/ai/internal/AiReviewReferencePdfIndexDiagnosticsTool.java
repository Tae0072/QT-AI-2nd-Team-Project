package com.qtai.domain.ai.internal;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class AiReviewReferencePdfIndexDiagnosticsTool {

    private AiReviewReferencePdfIndexDiagnosticsTool() {
    }

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        AiReviewReferencePdfIndexCandidateGenerator generator = new AiReviewReferencePdfIndexCandidateGenerator(
                new AiReviewReferencePdfHeadingParser(),
                new AiReviewReferenceTextQualityAnalyzer()
        );
        AiReviewReferencePdfIndexCandidateWriter writer =
                new AiReviewReferencePdfIndexCandidateWriter(new ObjectMapper());

        AiReviewReferencePdfIndexCandidateWriter.CandidateDocument document =
                generator.generate(arguments.source());
        writer.write(document, arguments.output(), arguments.summary());

        System.out.printf(
                "AI review reference PDF diagnostics completed. entries=%d usable=%d needsReview=%d unusable=%d%n",
                document.qualitySummary().totalEntryCount(),
                document.qualitySummary().usableEntryCount(),
                document.qualitySummary().needsReviewEntryCount(),
                document.qualitySummary().unusableEntryCount()
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
                    "Usage: --source <pdf> --output <candidate-json> --summary <summary-json>"
            );
        }

        private static void requireBuildOutput(Path outputPath) {
            Path buildRoot = Path.of("build").toAbsolutePath().normalize();
            Path normalizedOutput = outputPath.toAbsolutePath().normalize();
            if (!normalizedOutput.startsWith(buildRoot)) {
                throw new IllegalArgumentException(
                        "AI_REVIEW_REFERENCE_PDF_OUTPUT_MUST_BE_UNDER_BUILD"
                );
            }
        }
    }
}
