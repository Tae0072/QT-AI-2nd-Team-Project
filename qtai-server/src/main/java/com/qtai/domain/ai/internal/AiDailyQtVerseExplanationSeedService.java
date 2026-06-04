package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
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
    private static final String ACTIVE_PROMPT_NOT_FOUND = "ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND";

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDailyQtVerseExplanationSeedResult seedToday() {
        TodayQtResponse todayQt = Objects.requireNonNull(
                getTodayQtUseCase.getToday(null),
                "todayQt must not be null"
        );
        if (todayQt.qtPassageId() == null) {
            return new AiDailyQtVerseExplanationSeedResult(0, 0);
        }
        Long qtPassageId = requirePositive(todayQt.qtPassageId(), "qtPassageId");
        QtPassageContentContext context = Objects.requireNonNull(
                getQtPassageContentContextUseCase.getContentContext(qtPassageId),
                "qtPassageContentContext must not be null"
        );
        List<Long> verseIds = uniqueVerseIds(context.verseIds());
        if (verseIds.isEmpty()) {
            return new AiDailyQtVerseExplanationSeedResult(0, 0);
        }

        Set<Long> skippedVerseIds = skippedVerseIds(verseIds);
        List<Long> targetVerseIds = verseIds.stream()
                .filter(verseId -> !skippedVerseIds.contains(verseId))
                .toList();
        if (targetVerseIds.isEmpty()) {
            return new AiDailyQtVerseExplanationSeedResult(0, 0);
        }

        AiPromptVersion promptVersion = latestActiveExplanationPromptVersion();
        if (promptVersion == null) {
            log.warn("AI daily QT verse explanation seed skipped. reason={}", ACTIVE_PROMPT_NOT_FOUND);
            return new AiDailyQtVerseExplanationSeedResult(0, 0, ACTIVE_PROMPT_NOT_FOUND);
        }

        int createdCount = 0;
        int failedCount = 0;
        for (Long verseId : targetVerseIds) {
            try {
                createAiGenerationJobUseCase.createAiGenerationJob(new CreateAiGenerationJobCommand(
                        AiGenerationJobType.EXPLANATION.name(),
                        AiTargetType.BIBLE_VERSE.name(),
                        verseId,
                        promptVersion.getId(),
                        SYSTEM_BATCH,
                        OffsetDateTime.now(clock)
                ));
                createdCount++;
            } catch (BusinessException exception) {
                if (exception.getErrorCode() == ErrorCode.INVALID_STATUS_TRANSITION) {
                    log.info(
                            "AI daily QT verse explanation seed skipped for verse. verseId={}, reason={}",
                            verseId,
                            "DUPLICATE_ACTIVE_GENERATION_JOB"
                    );
                    continue;
                }
                failedCount++;
                log.warn(
                        "AI daily QT verse explanation seed failed for verse. verseId={}, errorType={}, errorMessage={}",
                        verseId,
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                );
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn(
                        "AI daily QT verse explanation seed failed for verse. verseId={}, errorType={}, errorMessage={}",
                        verseId,
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                );
            }
        }

        log.info(
                "AI daily QT verse explanation seed created jobs. qtPassageId={}, verseCount={}, skippedCount={}, createdCount={}, failedCount={}",
                qtPassageId,
                verseIds.size(),
                skippedVerseIds.size(),
                createdCount,
                failedCount
        );
        return new AiDailyQtVerseExplanationSeedResult(createdCount, failedCount);
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
        skippedVerseIds.addAll(generatedAssetRepository.findReadyExplanationBibleVerseTargetIds(verseIds));
        skippedVerseIds.addAll(generationJobRepository.findActiveExplanationBibleVerseTargetIds(verseIds));
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
