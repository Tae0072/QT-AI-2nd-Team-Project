package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;

@ExtendWith(OutputCaptureExtension.class)
class AiDailyQtVerseExplanationSeedServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-31T15:05:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-06-01T00:05:00+09:00");

    private GetTodayQtUseCase getTodayQtUseCase;
    private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase;
    private AiPromptVersionRepository promptVersionRepository;
    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiGenerationJobRepository generationJobRepository;
    private CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    private AiDailyQtVerseExplanationSeedService service;

    @BeforeEach
    void setUp() {
        getTodayQtUseCase = mock(GetTodayQtUseCase.class);
        getQtPassageContentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        listApprovedVerseExplanationUseCase = mock(ListApprovedVerseExplanationUseCase.class);
        promptVersionRepository = mock(AiPromptVersionRepository.class);
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
        generationJobRepository = mock(AiGenerationJobRepository.class);
        createAiGenerationJobUseCase = mock(CreateAiGenerationJobUseCase.class);
        service = new AiDailyQtVerseExplanationSeedService(
                getTodayQtUseCase,
                getQtPassageContentContextUseCase,
                listApprovedVerseExplanationUseCase,
                promptVersionRepository,
                generatedAssetRepository,
                generationJobRepository,
                createAiGenerationJobUseCase,
                CLOCK
        );
    }

    @Test
    void seedTodayCreatesJobsOnlyForEligibleUniqueVerseIds() {
        AiPromptVersion promptVersion = promptVersion(15L, "2026.06.1", REQUESTED_AT.minusDays(1));
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context());
        when(promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).thenReturn(Optional.of(promptVersion));
        when(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of(new ApprovedVerseExplanationResponse(
                        101L,
                        "summary",
                        "explanation",
                        "source",
                        900L
                )));
        when(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of(102L));
        when(generationJobRepository.findActiveExplanationBibleVerseTargetIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of(103L));
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenReturn(new CreateAiGenerationJobResult(501L, "QUEUED"));

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        ArgumentCaptor<CreateAiGenerationJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAiGenerationJobCommand.class);
        verify(createAiGenerationJobUseCase).createAiGenerationJob(commandCaptor.capture());
        CreateAiGenerationJobCommand command = commandCaptor.getValue();
        assertThat(command.jobType()).isEqualTo("EXPLANATION");
        assertThat(command.targetType()).isEqualTo("BIBLE_VERSE");
        assertThat(command.targetId()).isEqualTo(104L);
        assertThat(command.promptVersionId()).isEqualTo(15L);
        assertThat(command.requestedBy()).isEqualTo("SYSTEM_BATCH");
        assertThat(command.requestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void seedTodayContinuesWhenOneVerseJobCreationFails(CapturedOutput output) {
        AiPromptVersion promptVersion = promptVersion(15L, "2026.06.1", REQUESTED_AT.minusDays(1));
        List<Long> verseIds = List.of(101L, 102L, 103L, 104L, 105L);
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context(verseIds));
        when(promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).thenReturn(Optional.of(promptVersion));
        when(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseIds))
                .thenReturn(List.of(new ApprovedVerseExplanationResponse(
                        101L,
                        "summary",
                        "explanation",
                        "source",
                        900L
                )));
        when(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(verseIds))
                .thenReturn(List.of(102L));
        when(generationJobRepository.findActiveExplanationBibleVerseTargetIds(verseIds))
                .thenReturn(List.of(103L));
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenAnswer(invocation -> {
                    CreateAiGenerationJobCommand command = invocation.getArgument(0);
                    if (command.targetId().equals(104L)) {
                        throw new IllegalStateException("duplicate queued job");
                    }
                    return new CreateAiGenerationJobResult(502L, "QUEUED");
                });

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        ArgumentCaptor<CreateAiGenerationJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAiGenerationJobCommand.class);
        verify(createAiGenerationJobUseCase, times(2)).createAiGenerationJob(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(CreateAiGenerationJobCommand::targetId)
                .containsExactly(104L, 105L);
        assertThat(output).contains(
                "AI daily QT verse explanation seed failed for verse",
                "verseId=104",
                "errorType=IllegalStateException",
                "errorMessage=duplicate queued job"
        );
    }

    @Test
    void seedTodayTreatsDuplicateActiveJobRaceAsSkipped(CapturedOutput output) {
        AiPromptVersion promptVersion = promptVersion(15L, "2026.06.1", REQUESTED_AT.minusDays(1));
        List<Long> verseIds = List.of(104L, 105L);
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context(verseIds));
        when(promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).thenReturn(Optional.of(promptVersion));
        when(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseIds)).thenReturn(List.of());
        when(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(verseIds)).thenReturn(List.of());
        when(generationJobRepository.findActiveExplanationBibleVerseTargetIds(verseIds)).thenReturn(List.of());
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenAnswer(invocation -> {
                    CreateAiGenerationJobCommand command = invocation.getArgument(0);
                    if (command.targetId().equals(104L)) {
                        throw new BusinessException(
                                ErrorCode.INVALID_STATUS_TRANSITION,
                                "duplicate active generation job"
                        );
                    }
                    return new CreateAiGenerationJobResult(503L, "QUEUED");
                });

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        ArgumentCaptor<CreateAiGenerationJobCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateAiGenerationJobCommand.class);
        verify(createAiGenerationJobUseCase, times(2)).createAiGenerationJob(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues())
                .extracting(CreateAiGenerationJobCommand::targetId)
                .containsExactly(104L, 105L);
        assertThat(output).contains(
                "AI daily QT verse explanation seed skipped for verse",
                "verseId=104",
                "reason=DUPLICATE_ACTIVE_GENERATION_JOB"
        );
    }

    @Test
    void seedTodayReturnsZeroWhenVerseIdsAreEmpty() {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context(List.of()));

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isZero();
        assertThat(result.failedCount()).isZero();
        verifyNoInteractions(
                promptVersionRepository,
                listApprovedVerseExplanationUseCase,
                generatedAssetRepository,
                generationJobRepository,
                createAiGenerationJobUseCase
        );
    }

    @Test
    void seedTodayReturnsZeroWhenTodayQtHasNoPassage() {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt(null));

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isZero();
        assertThat(result.failedCount()).isZero();
        verifyNoInteractions(
                getQtPassageContentContextUseCase,
                promptVersionRepository,
                listApprovedVerseExplanationUseCase,
                generatedAssetRepository,
                generationJobRepository,
                createAiGenerationJobUseCase
        );
    }

    @Test
    void seedTodayReturnsZeroWhenEveryVerseAlreadyHasApprovedExplanation() {
        List<Long> verseIds = List.of(101L, 102L);
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context(verseIds));
        when(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseIds))
                .thenReturn(List.of(
                        new ApprovedVerseExplanationResponse(101L, "summary-101", "explanation-101", "source", 901L),
                        new ApprovedVerseExplanationResponse(102L, "summary-102", "explanation-102", "source", 902L)
                ));
        when(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(verseIds)).thenReturn(List.of());
        when(generationJobRepository.findActiveExplanationBibleVerseTargetIds(verseIds)).thenReturn(List.of());

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isZero();
        assertThat(result.failedCount()).isZero();
        verifyNoInteractions(promptVersionRepository, createAiGenerationJobUseCase);
    }

    @Test
    void seedTodayDoesNotCreateJobsWhenActiveExplanationPromptVersionIsMissing(CapturedOutput output) {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context());
        when(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of());
        when(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of());
        when(generationJobRepository.findActiveExplanationBibleVerseTargetIds(List.of(101L, 102L, 103L, 104L)))
                .thenReturn(List.of());
        when(promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).thenReturn(Optional.empty());

        AiDailyQtVerseExplanationSeedResult result = service.seedToday();

        assertThat(result.createdCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.failureReason()).isEqualTo("ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND");
        assertThat(output).contains("ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND");
        verify(createAiGenerationJobUseCase, never())
                .createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
    }

    @Test
    void seedTodayRejectsInvalidQtPassageId() {
        assertInvalidQtPassageId(0L);
        assertInvalidQtPassageId(-1L);
    }

    @Test
    void seedTodayRejectsInvalidVerseId() {
        assertInvalidVerseIds(List.of(0L));
        assertInvalidVerseIds(List.of(-1L));
        assertInvalidVerseIds(Arrays.asList(101L, null));
    }

    @Test
    void seedTodayRequiresTodayQtResponse() {
        when(getTodayQtUseCase.getToday(null)).thenReturn(null);

        assertThatThrownBy(service::seedToday)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("todayQt must not be null");
    }

    @Test
    void seedTodayRequiresQtPassageContentContext() {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(null);

        assertThatThrownBy(service::seedToday)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("qtPassageContentContext must not be null");
    }

    private void assertInvalidQtPassageId(Long qtPassageId) {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt(qtPassageId));

        assertThatThrownBy(service::seedToday)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private void assertInvalidVerseIds(List<Long> verseIds) {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context(verseIds));

        assertThatThrownBy(service::seedToday)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private static TodayQtResponse todayQt() {
        return todayQt(35L);
    }

    private static TodayQtResponse todayQt(Long qtPassageId) {
        return new TodayQtResponse(
                qtPassageId,
                "2026-06-01",
                "Daily QT",
                "MISSING",
                false,
                null,
                "HIT"
        );
    }

    private static QtPassageContentContext context() {
        return context(List.of(101L, 102L, 103L, 104L, 104L));
    }

    private static QtPassageContentContext context(List<Long> verseIds) {
        return new QtPassageContentContext(
                35L,
                LocalDate.parse("2026-06-01"),
                "Daily QT",
                verseIds,
                true
        );
    }

    private static AiPromptVersion promptVersion(Long id, String version, OffsetDateTime createdAt) {
        return AiPromptVersion.of(
                id,
                AiPromptType.EXPLANATION,
                version,
                "hash-" + version,
                AiPromptVersionStatus.ACTIVE,
                createdAt
        );
    }
}
