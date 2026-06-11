package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.ai.AiServiceApplication;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_event_outbox_persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
@Transactional
class AiServiceEventOutboxPersistenceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private AiEventOutboxRepository aiEventOutboxRepository;

    @Autowired
    private AiProcessedEventRepository aiProcessedEventRepository;

    @Test
    void savesAndQueriesPendingOutboxEventsInCreationOrder() {
        AiEventOutbox second = aiEventOutboxRepository.save(outbox(
                "00000000-0000-0000-0000-000000000202",
                BASE_TIME.plusMinutes(2)
        ));
        AiEventOutbox first = aiEventOutboxRepository.save(outbox(
                "00000000-0000-0000-0000-000000000201",
                BASE_TIME.plusMinutes(1)
        ));
        AiEventOutbox published = outbox(
                "00000000-0000-0000-0000-000000000203",
                BASE_TIME.plusMinutes(3)
        );
        published.markPublished(BASE_TIME.plusMinutes(4));
        aiEventOutboxRepository.saveAndFlush(published);

        assertThat(aiEventOutboxRepository.findByStatusOrderByCreatedAtAscIdAsc(
                AiEventOutboxStatus.PENDING,
                PageRequest.of(0, 10)
        )).extracting(AiEventOutbox::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void savesPublishedAndFailedOutboxStates() {
        AiEventOutbox published = aiEventOutboxRepository.saveAndFlush(outbox(
                "00000000-0000-0000-0000-000000000204",
                BASE_TIME
        ));
        AiEventOutbox failed = aiEventOutboxRepository.saveAndFlush(outbox(
                "00000000-0000-0000-0000-000000000205",
                BASE_TIME.plusMinutes(1)
        ));

        published.markPublished(BASE_TIME.plusMinutes(2));
        failed.markFailed("DOWNSTREAM_ERROR", "provider call failed");
        aiEventOutboxRepository.saveAndFlush(published);
        aiEventOutboxRepository.saveAndFlush(failed);

        assertThat(aiEventOutboxRepository.findByEventId(published.getEventId()))
                .map(AiEventOutbox::getStatus)
                .contains(AiEventOutboxStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isEqualTo(BASE_TIME.plusMinutes(2));
        assertThat(aiEventOutboxRepository.findByEventId(failed.getEventId()))
                .map(AiEventOutbox::getStatus)
                .contains(AiEventOutboxStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastErrorCode()).isEqualTo("DOWNSTREAM_ERROR");
    }

    @Test
    void rejectsForbiddenPayloadFields() {
        String forbiddenPayload = "{\"" + "prompt" + "\":\"blocked test value\"}";

        assertThatThrownBy(() -> AiEventOutbox.create(
                "00000000-0000-0000-0000-000000000206",
                "AiGenerationJobRequested",
                "ai_generation_job",
                "job-1001",
                "0.1.0",
                forbiddenPayload,
                "trace-ai-1001",
                BASE_TIME
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson must not store forbidden provider or validation reference fields");
    }

    @Test
    void processedEventUsesEventIdAndHandlerNameAsIdempotencyKey() {
        AiProcessedEvent firstHandler = aiProcessedEventRepository.saveAndFlush(AiProcessedEvent.succeeded(
                "00000000-0000-0000-0000-000000000207",
                "generation-worker",
                "ai_generation_job",
                "job-1001",
                BASE_TIME
        ));
        AiProcessedEvent secondHandler = aiProcessedEventRepository.saveAndFlush(AiProcessedEvent.succeeded(
                "00000000-0000-0000-0000-000000000207",
                "monitoring-handler",
                "ai_generation_job",
                "job-1001",
                BASE_TIME.plusMinutes(1)
        ));

        assertThat(firstHandler.getId()).isNotNull();
        assertThat(secondHandler.getId()).isNotNull();
        assertThat(aiProcessedEventRepository.existsByEventIdAndHandlerName(
                "00000000-0000-0000-0000-000000000207",
                "generation-worker"
        )).isTrue();
        assertThat(aiProcessedEventRepository.findByEventIdAndHandlerName(
                "00000000-0000-0000-0000-000000000207",
                "monitoring-handler"
        )).map(AiProcessedEvent::getStatus).contains(AiProcessedEventStatus.SUCCEEDED);
    }

    @Test
    void rejectsDuplicateProcessedEventForSameHandler() {
        aiProcessedEventRepository.saveAndFlush(AiProcessedEvent.succeeded(
                "00000000-0000-0000-0000-000000000208",
                "generation-worker",
                "ai_generation_job",
                "job-1001",
                BASE_TIME
        ));

        assertThatThrownBy(() -> aiProcessedEventRepository.saveAndFlush(AiProcessedEvent.failed(
                "00000000-0000-0000-0000-000000000208",
                "generation-worker",
                "ai_generation_job",
                "job-1001",
                "DOWNSTREAM_ERROR",
                "provider call failed",
                BASE_TIME.plusMinutes(1)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static AiEventOutbox outbox(String eventId, OffsetDateTime createdAt) {
        return AiEventOutbox.create(
                eventId,
                "AiGenerationJobRequested",
                "ai_generation_job",
                "job-1001",
                "0.1.0",
                "{\"jobId\":1001,\"passageId\":35}",
                "trace-ai-1001",
                createdAt
        );
    }
}
