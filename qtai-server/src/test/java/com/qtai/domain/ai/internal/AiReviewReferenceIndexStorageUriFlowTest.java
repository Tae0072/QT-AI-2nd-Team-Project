package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AiReviewReferenceIndexStorageUriFlowTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-05T09:00:00+09:00");
    private static final String SOURCE_FILE_HASH = "sha256:synthetic-reference-hash";
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private ValidationReferenceJobRepository repository;

    @TempDir
    Path restrictedRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dbMetadataIndexStorageUriReadsRestrictedIndexAndSelectsMatchingEntry() throws Exception {
        writeSyntheticIndex();
        ValidationReferenceJob saved = repository.saveAndFlush(ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                SOURCE_FILE_HASH,
                "restricted://validation/reference.pdf",
                INDEX_STORAGE_URI,
                CREATED_AT.plusDays(1),
                CREATED_AT
        ));
        testEntityManager.clear();

        AiReviewReferenceService referenceService = referenceService();
        AiReviewReferenceService.ReferenceMetadata metadata = referenceService.latestActiveReference().orElseThrow();

        AiReviewReferenceIndexReader reader = new AiReviewReferenceIndexReader(
                new RestrictedStorageUriResolver(restrictedRoot),
                objectMapper
        );
        AiReviewReferenceIndexReader.ReferenceIndex index = reader.read(
                metadata.indexStorageUri(),
                metadata.sourceFileHash()
        );

        List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts =
                new AiReviewReferenceExcerptSelector(objectMapper).select(assetPayloadJson(), index);

        assertThat(metadata.validationReferenceJobId()).isEqualTo(saved.getId());
        assertThat(metadata.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(metadata.indexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(index.entries()).hasSize(2);
        assertThat(selectedExcerpts).hasSize(1);
        AiReviewReferenceExcerptSelector.SelectedExcerpt selectedExcerpt = selectedExcerpts.get(0);
        assertThat(selectedExcerpt.bookCode()).isEqualTo("JHN");
        assertThat(selectedExcerpt.referenceHash()).isEqualTo("sha256:synthetic-jhn-entry");
        assertThat(selectedExcerpt.referenceText()).isEqualTo("synthetic reviewer note for matching verse");
    }

    private AiReviewReferenceService referenceService() {
        return new AiReviewReferenceService(repository);
    }

    private void writeSyntheticIndex() throws Exception {
        Path indexPath = restrictedRoot.resolve("validation").resolve("index").resolve("reference-index.json");
        Files.createDirectories(indexPath.getParent());
        Files.writeString(indexPath, """
                {
                  "schemaVersion": "ai-review-reference-index.v1",
                  "sourceFileHash": "sha256:synthetic-reference-hash",
                  "generatedAt": "2026-06-05T09:00:00+09:00",
                  "entries": [
                    {
                      "bookCode": "JHN",
                      "chapterStart": 3,
                      "verseStart": 16,
                      "chapterEnd": 3,
                      "verseEnd": 18,
                      "referenceRangeLabel": "JHN 3:16-18",
                      "referenceText": "synthetic reviewer note for matching verse",
                      "referenceHash": "sha256:synthetic-jhn-entry"
                    },
                    {
                      "bookCode": "ROM",
                      "chapterStart": 1,
                      "verseStart": 1,
                      "chapterEnd": 1,
                      "verseEnd": 2,
                      "referenceRangeLabel": "ROM 1:1-2",
                      "referenceText": "synthetic reviewer note for non matching verse",
                      "referenceHash": "sha256:synthetic-rom-entry"
                    }
                  ]
                }
                """);
    }

    private static String assetPayloadJson() {
        return """
                {
                  "sourceMetadata": {
                    "verses": [
                      {"bookCode": "JHN", "chapterNo": 3, "verseNo": 16}
                    ]
                  }
                }
                """;
    }
}
