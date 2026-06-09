package com.qtai.domain.ai.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiEventContractFixtureTest {

    private static final String FIXTURE_RESOURCE = "/contracts/ai-events/ai-event-contract-fixtures.json";

    private static final List<String> EXPECTED_EVENT_NAMES = List.of(
            "AiGenerationJobRequested",
            "AiGenerationJobStarted",
            "AiGenerationJobCompleted",
            "AiGenerationJobFailed",
            "AiValidationCompleted",
            "AiAssetApproved",
            "AiAssetPublishRequested",
            "AiAssetPublishFailed",
            "ProviderAiInputPrepared"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode fixtures;

    @BeforeEach
    void setUp() throws IOException {
        fixtures = loadFixtures();
    }

    @Test
    void eventCatalogContainsExpectedEventsOnly() {
        JsonNode events = fixtures.path("events");

        assertThat(fieldNames(events)).containsExactlyElementsOf(EXPECTED_EVENT_NAMES);
    }

    @Test
    void allEventsContainCommonEnvelopeFields() {
        List<String> commonFields = stringList(fixtures.path("commonRequiredFields"));
        JsonNode events = fixtures.path("events");
        String schemaVersion = fixtures.path("schemaVersion").asText();

        EXPECTED_EVENT_NAMES.forEach(eventName -> {
            JsonNode event = events.path(eventName);

            commonFields.forEach(fieldName ->
                    assertThat(event.hasNonNull(fieldName))
                            .as("%s must contain common field %s", eventName, fieldName)
                            .isTrue()
            );
            assertThat(event.path("schemaVersion").asText()).isEqualTo(schemaVersion);
            assertThat(event.path("eventName").asText()).isEqualTo(eventName);
            assertThatCodeParsesAsUuid(event.path("eventId").asText());
            assertThat(event.path("aggregateType").asText()).isNotBlank();
            assertThat(event.path("aggregateId").asText()).isNotBlank();
            assertThat(event.path("traceId").asText()).isNotBlank();
            assertThat(event.path("traceparent").asText())
                    .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01");
            assertThat(OffsetDateTime.parse(event.path("occurredAt").asText())).isNotNull();
            assertThat(event.path("payload").isObject()).isTrue();
        });
    }

    @Test
    void eventPayloadsContainRequiredFields() {
        JsonNode requiredFieldsByEvent = fixtures.path("eventPayloadRequiredFields");
        JsonNode events = fixtures.path("events");

        EXPECTED_EVENT_NAMES.forEach(eventName -> {
            JsonNode payload = events.path(eventName).path("payload");
            stringList(requiredFieldsByEvent.path(eventName)).forEach(fieldName ->
                    assertThat(payload.hasNonNull(fieldName))
                            .as("%s payload must contain %s", eventName, fieldName)
                            .isTrue()
            );
        });
    }

    @Test
    void validEventPayloadsDoNotContainForbiddenFields() {
        Set<String> forbiddenFields = new TreeSet<>(stringList(fixtures.path("forbiddenPayloadFieldNames")));
        JsonNode events = fixtures.path("events");

        EXPECTED_EVENT_NAMES.forEach(eventName -> {
            Set<String> payloadFieldNames = new TreeSet<>();
            collectFieldNames(events.path(eventName).path("payload"), payloadFieldNames);

            assertThat(payloadFieldNames)
                    .as("%s payload must not contain forbidden fields", eventName)
                    .doesNotContainAnyElementsOf(forbiddenFields);
        });
    }

    @Test
    void publishRequestedEventRequiresIdempotencyKey() {
        JsonNode payload = fixtures
                .path("events")
                .path("AiAssetPublishRequested")
                .path("payload");

        assertThat(payload.path("idempotencyKey").asText())
                .isNotBlank()
                .startsWith("ai-event-fixture-publish-");
    }

    @Test
    void qtBibleReferenceLookupKeepsHttpUntilProviderInputEventIsApproved() {
        JsonNode decisions = fixtures.path("decisions");

        assertThat(decisions.path("qtBibleReferenceLookup").asText())
                .isEqualTo("HTTP_CLIENT_CONTRACT_UNTIL_PROVIDER_AI_INPUT_EVENT_APPROVED");
        assertThat(decisions.path("providerInputEventContext").asText())
                .isEqualTo("ALLOWED_CONTEXT_BLOCK_ONLY");
        assertThat(fieldNames(fixtures.path("events").path("ProviderAiInputPrepared").path("payload")))
                .contains("contextBlockType")
                .doesNotContain("contextText");
    }

    private JsonNode loadFixtures() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(inputStream)
                    .as("fixture resource must exist: %s", FIXTURE_RESOURCE)
                    .isNotNull();
            return objectMapper.readTree(inputStream);
        }
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private static void collectFieldNames(JsonNode node, Set<String> fieldNames) {
        if (node.isObject()) {
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String fieldName = names.next();
                fieldNames.add(fieldName);
                collectFieldNames(node.path(fieldName), fieldNames);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFieldNames(child, fieldNames));
        }
    }

    private static void assertThatCodeParsesAsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
