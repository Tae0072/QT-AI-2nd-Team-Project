package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;

@Slf4j
@Service
class AiDailyQtVerseExplanationSeedService {

    private static final String SYSTEM_BATCH = "SYSTEM_BATCH";
    private static final List<AiGeneratedAssetStatus> READY_ASSET_STATUSES = List.of(
            AiGeneratedAssetStatus.VALIDATING,
            AiGeneratedAssetStatus.APPROVED
    );
    private static final List<AiGenerationJobStatus> ACTIVE_JOB_STATUSES = List.of(
            AiGenerationJobStatus.QUEUED,
            AiGenerationJobStatus.RUNNING
    );

    private final GetTodayQtUseCase getTodayQtUseCase;
    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase;
    private final AiPromptVersionRepository promptVersionRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiGenerationJobRepository generationJobRepository;
    private final CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    private final Clock clock;

    AiDailyQtVerseExplanationSeedService(
            GetTodayQtUseCase getTodayQtUseCase,
            GetQtPassageContentContextUseCase getQtPassageContentContextUseCase,
            ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase,
            AiPromptVersionRepository promptVersionRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiGenerationJobRepository generationJobRepository,
            CreateAiGenerationJobUseCase createAiGenerationJobUseCase,
            Clock clock
    ) {
        this.getTodayQtUseCase = getTodayQtUseCase;
        this.getQtPassageContentContextUseCase = getQtPassageContentContextUseCase;
        this.listApprovedVerseExplanationUseCase = listApprovedVerseExplanationUseCase;
        this.promptVersionRepository = promptVersionRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.generationJobRepository = generationJobRepository;
        this.createAiGenerationJobUseCase = createAiGenerationJobUseCase;
        this.clock = clock;
    }

    int seedToday() {
        TodayQtResponse todayQt = getTodayQtUseCase.getToday(null);
        Long qtPassageId = requirePositive(todayQt.qtPassageId(), "qtPassageId");
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        List<Long> verseIds = uniqueVerseIds(context.verseIds());
        if (verseIds.isEmpty()) {
            return 0;
        }

        AiPromptVersion promptVersion = latestActiveExplanationPromptVersion();
        if (promptVersion == null) {
            log.warn("AI daily QT verse explanation seed skipped. reason=ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND");
            return 0;
        }

        Set<Long> skippedVerseIds = skippedVerseIds(verseIds);
        int createdCount = 0;
        for (Long verseId : verseIds) {
            if (skippedVerseIds.contains(verseId)) {
                continue;
            }
            createAiGenerationJobUseCase.createAiGenerationJob(new CreateAiGenerationJobCommand(
                    AiGenerationJobType.EXPLANATION.name(),
                    AiTargetType.BIBLE_VERSE.name(),
                    verseId,
                    promptVersion.getId(),
                    SYSTEM_BATCH,
                    OffsetDateTime.now(clock)
            ));
            createdCount++;
        }

        log.info(
                "AI daily QT verse explanation seed created jobs. qtPassageId={}, verseCount={}, skippedCount={}, createdCount={}",
                qtPassageId,
                verseIds.size(),
                skippedVerseIds.size(),
                createdCount
        );
        return createdCount;
    }

    private AiPromptVersion latestActiveExplanationPromptVersion() {
        return promptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                        AiPromptType.EXPLANATION,
                        AiPromptVersionStatus.ACTIVE
                )
                .orElse(null);
    }

    private Set<Long> skippedVerseIds(List<Long> verseIds) {
        Set<Long> skippedVerseIds = new LinkedHashSet<>();
        listApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseIds)
                .stream()
                .map(ApprovedVerseExplanationResponse::verseId)
                .filter(Objects::nonNull)
                .forEach(skippedVerseIds::add);
        skippedVerseIds.addAll(generatedAssetRepository.findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusIn(
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                verseIds,
                READY_ASSET_STATUSES
        ));
        skippedVerseIds.addAll(generationJobRepository.findTargetIdsByJobTypeAndTargetTypeAndTargetIdInAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                verseIds,
                ACTIVE_JOB_STATUSES
        ));
        return skippedVerseIds;
    }

    private static List<Long> uniqueVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueVerseIds = new LinkedHashSet<>();
        for (Long verseId : verseIds) {
            uniqueVerseIds.add(requirePositive(verseId, "verseId"));
        }
        return List.copyOf(uniqueVerseIds);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }
}
