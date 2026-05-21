package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiGenerationJobTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T04:00:00+09:00");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-05-20T04:01:00+09:00");
    private static final OffsetDateTime FINISHED_AT = OffsetDateTime.parse("2026-05-20T04:02:00+09:00");

    @Test
    void failedJobCannotSucceed() {
        AiGenerationJob job = newJob();
        job.markFailed("LLM_TIMEOUT", FINISHED_AT);

        assertThatThrownBy(() -> job.markSucceeded(FINISHED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("LLM_TIMEOUT");
    }

    @Test
    void succeededJobCannotFail() {
        AiGenerationJob job = newJob();
        job.markRunning(STARTED_AT);
        job.markSucceeded(FINISHED_AT);

        assertThatThrownBy(() -> job.markFailed("LATE_FAILURE", FINISHED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    void succeededJobCannotRunAgain() {
        AiGenerationJob job = newJob();
        job.markRunning(STARTED_AT);
        job.markSucceeded(FINISHED_AT);

        assertThatThrownBy(() -> job.markRunning(FINISHED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
    }

    @Test
    void queuedJobCanFail() {
        AiGenerationJob job = newJob();

        job.markFailed("INPUT_BLOCKED", FINISHED_AT);

        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getFinishedAt()).isEqualTo(FINISHED_AT);
    }

    @Test
    void queuedAndRunningJobsKeepActiveUniqueKeyUntilTerminalState() {
        AiGenerationJob job = newJob();

        assertThat(job.getActiveUniqueKey()).isEqualTo("ACTIVE");

        job.markRunning(STARTED_AT);

        assertThat(job.getActiveUniqueKey()).isEqualTo("ACTIVE");

        job.markSucceeded(FINISHED_AT);

        assertThat(job.getActiveUniqueKey()).isNull();
    }

    @Test
    void failedJobClearsActiveUniqueKeyForRetry() {
        AiGenerationJob job = newJob();

        job.markFailed("INPUT_BLOCKED", FINISHED_AT);

        assertThat(job.getActiveUniqueKey()).isNull();
    }

    private static AiGenerationJob newJob() {
        return AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "2026.05.1",
                CREATED_AT
        );
    }
}
