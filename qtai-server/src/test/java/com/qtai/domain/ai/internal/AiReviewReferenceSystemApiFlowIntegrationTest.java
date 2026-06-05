package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiReviewReferenceSystemApiFlowIntegrationTest {

    private static final String SOURCE_FILE_HASH = "sha256:reference-hash";
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;

    @Autowired
    private AiReviewReferenceService referenceService;

    @BeforeEach
    void setUp() {
        validationReferenceJobRepository.deleteAll();
        validationReferenceJobRepository.flush();
    }

    @Test
    void systemApiPostStoresIndexStorageUriAndLatestReferenceMetadataReadsIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/system/validation-reference-jobs")
                        .with(user("batch")
                                .authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_BATCH")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.sourceName").value("검증 참조 자료"))
                .andExpect(jsonPath("$.data.sourceFileName").value("reference-notes.pdf"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.sourceFileHash").doesNotExist())
                .andExpect(jsonPath("$.data.storageUri").doesNotExist())
                .andExpect(jsonPath("$.data.indexStorageUri").doesNotExist())
                .andReturn();

        Long jobId = responseJobId(result);
        ValidationReferenceJob job = validationReferenceJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getSourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(job.getStorageUri()).isEqualTo("restricted://validation/reference.pdf");
        assertThat(job.getIndexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(job.getStatus()).isEqualTo(ValidationReferenceJobStatus.ACTIVE);

        AiReviewReferenceService.ReferenceMetadata metadata =
                referenceService.latestActiveReference().orElseThrow();
        assertThat(metadata.validationReferenceJobId()).isEqualTo(jobId);
        assertThat(metadata.sourceName()).isEqualTo("검증 참조 자료");
        assertThat(metadata.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(metadata.indexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
    }

    private Long responseJobId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private static String validRequestBody() {
        return """
                {
                  "sourceName": "검증 참조 자료",
                  "sourceFileName": "reference-notes.pdf",
                  "sourceFileHash": "%s",
                  "storageUri": "restricted://validation/reference.pdf",
                  "indexStorageUri": "%s",
                  "expiresAt": "2026-05-29T04:00:00+09:00"
                }
                """.formatted(SOURCE_FILE_HASH, INDEX_STORAGE_URI);
    }
}
