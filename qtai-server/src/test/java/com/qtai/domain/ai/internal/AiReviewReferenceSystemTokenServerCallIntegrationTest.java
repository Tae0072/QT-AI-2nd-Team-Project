package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.security.JwtProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AiReviewReferenceSystemTokenServerCallIntegrationTest {

    private static final String ENDPOINT = "/api/v1/system/validation-reference-jobs";
    private static final String SOURCE_FILE_HASH = "sha256:reference-hash";
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;

    @Autowired
    private AiReviewReferenceService referenceService;

    @BeforeEach
    void setUp() {
        clearReferenceJobs();
    }

    @AfterEach
    void tearDown() {
        clearReferenceJobs();
    }

    @Test
    void systemBatchJwtCanCallServerApiAndPersistIndexStorageUri() throws Exception {
        ResponseEntity<String> response = postWithToken(systemBatchToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("data").path("sourceName").asText()).isEqualTo("검증 참조 자료");
        assertThat(root.path("data").path("sourceFileName").asText()).isEqualTo("reference-notes.pdf");
        assertThat(root.path("data").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(root.path("data").has("sourceFileHash")).isFalse();
        assertThat(root.path("data").has("storageUri")).isFalse();
        assertThat(root.path("data").has("indexStorageUri")).isFalse();

        Long jobId = root.path("data").path("id").asLong();
        ValidationReferenceJob job = validationReferenceJobRepository.findById(jobId).orElseThrow();
        assertThat(job.getSourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(job.getStorageUri()).isEqualTo("restricted://validation/reference.pdf");
        assertThat(job.getIndexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(job.getStatus()).isEqualTo(ValidationReferenceJobStatus.ACTIVE);

        AiReviewReferenceService.ReferenceMetadata metadata =
                referenceService.latestActiveReference().orElseThrow();
        assertThat(metadata.validationReferenceJobId()).isEqualTo(jobId);
        assertThat(metadata.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(metadata.indexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
    }

    @Test
    void serverApiRejectsMissingToken() throws Exception {
        ResponseEntity<String> response = getWithToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode(response)).isEqualTo("M0002");
        assertThat(validationReferenceJobRepository.findAll()).isEmpty();
    }

    @Test
    void serverApiRejectsTamperedToken() throws Exception {
        ResponseEntity<String> response = getWithToken(tampered(systemBatchToken()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode(response)).isEqualTo("M0002");
        assertThat(validationReferenceJobRepository.findAll()).isEmpty();
    }

    @Test
    void serverApiRejectsUserRoleJwt() throws Exception {
        ResponseEntity<String> response = getWithToken(jwtProvider.issueAccessToken(1L, "USER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode(response)).isEqualTo("M0003");
        assertThat(validationReferenceJobRepository.findAll()).isEmpty();
    }

    private ResponseEntity<String> postWithToken(String token) {
        return restTemplate.postForEntity(ENDPOINT, requestWithToken(token), String.class);
    }

    private ResponseEntity<String> getWithToken(String token) {
        return restTemplate.exchange(ENDPOINT + "/1", HttpMethod.GET, emptyRequest(token), String.class);
    }

    private HttpEntity<String> requestWithToken(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(validRequestBody(), headers);
    }

    private HttpEntity<Void> emptyRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(headers);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String systemBatchToken() {
        return jwtProvider.issueAccessToken(1L, "SYSTEM_BATCH");
    }

    private static String tampered(String token) {
        return token.substring(0, token.length() - 8) + "tampered";
    }

    private String errorCode(ResponseEntity<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("error").path("code").asText();
    }

    private void clearReferenceJobs() {
        validationReferenceJobRepository.deleteAll();
        validationReferenceJobRepository.flush();
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
