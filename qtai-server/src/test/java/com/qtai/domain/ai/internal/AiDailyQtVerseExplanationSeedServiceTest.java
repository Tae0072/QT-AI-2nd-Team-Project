package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;

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
        when(generatedAssetRepository.findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusIn(
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                List.of(101L, 102L, 103L, 104L),
                List.of(AiGeneratedAssetStatus.VALIDATING, AiGeneratedAssetStatus.APPROVED)
        )).thenReturn(List.of(102L));
        when(generationJobRepository.findTargetIdsByJobTypeAndTargetTypeAndTargetIdInAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                List.of(101L, 102L, 103L, 104L),
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(List.of(103L));
        when(createAiGenerationJobUseCase.createAiGenerationJob(any(CreateAiGenerationJobCommand.class)))
                .thenReturn(new CreateAiGenerationJobResult(501L, "QUEUED"));

        int createdCount = service.seedToday();

        assertThat(createdCount).isEqualTo(1);
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
    void seedTodayDoesNotCreateJobsWhenActiveExplanationPromptVersionIsMissing() {
        when(getTodayQtUseCase.getToday(null)).thenReturn(todayQt());
        when(getQtPassageContentContextUseCase.getContentContext(35L)).thenReturn(context());
        when(promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).thenReturn(Optional.empty());

        int createdCount = service.seedToday();

        assertThat(createdCount).isZero();
        verify(createAiGenerationJobUseCase, never())
                .createAiGenerationJob(any(CreateAiGenerationJobCommand.class));
        verify(generatedAssetRepository, never())
                .findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusIn(any(), any(), any(), any());
        verify(generationJobRepository, never())
                .findTargetIdsByJobTypeAndTargetTypeAndTargetIdInAndStatusIn(any(), any(), any(), any());
    }

    private static TodayQtResponse todayQt() {
        return new TodayQtResponse(
                35L,
                "2026-06-01",
                "Daily QT",
                "MISSING",
                false,
                null,
                "HIT"
        );
    }

    private static QtPassageContentContext context() {
        return new QtPassageContentContext(
                35L,
                LocalDate.parse("2026-06-01"),
                "Daily QT",
                List.of(101L, 102L, 103L, 104L, 104L),
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
