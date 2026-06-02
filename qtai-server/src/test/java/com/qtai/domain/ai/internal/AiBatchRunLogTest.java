package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class AiBatchRunLogTest {

    @Test
    void redactsKeyValueSecretLikeErrorMessage() {
        AiBatchRunLog log = AiBatchRunLog.create(command("token=plain-secret-value"));

        assertThat(log.getErrorMessage()).isEqualTo("REDACTED_SENSITIVE_ERROR_MESSAGE");
    }

    @Test
    void doesNotRedactNormalWordsContainingToken() {
        AiBatchRunLog log = AiBatchRunLog.create(command("tokenizer failed while splitting text"));

        assertThat(log.getErrorMessage()).isEqualTo("tokenizer failed while splitting text");
    }

    @Test
    void redactsBearerAuthorizationHeader() {
        AiBatchRunLog log = AiBatchRunLog.create(command("Authorization: Bearer abc.def.ghi"));

        assertThat(log.getErrorMessage()).isEqualTo("REDACTED_SENSITIVE_ERROR_MESSAGE");
    }

    @Test
    void redactsMultilineBearerAuthorizationHeader() {
        AiBatchRunLog log = AiBatchRunLog.create(command("""
                handler failed while polling jobs
                Authorization: Bearer abc.def.ghi
                at com.qtai.domain.ai.internal.AiGenerationJobWorker.poll(AiGenerationJobWorker.java:42)
                """));

        assertThat(log.getErrorMessage()).isEqualTo("REDACTED_SENSITIVE_ERROR_MESSAGE");
    }

    private static AiBatchRunLogCommand command(String errorMessage) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        return new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                0,
                0,
                "IllegalStateException",
                errorMessage,
                now,
                now
        );
    }
}
