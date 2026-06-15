package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.LocalDate;
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
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;

@Slf4j
@Service
class AiDailyQtVerseExplanationSeedService {

    private static final String SYSTEM_BATCH = "SYSTEM_BATCH";
    private static final String ACTIVE_PROMPT_NOT_FOUND = "ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND";
    private static final String TODAY_PASSAGE_NOT_FOUND = "TODAY_QT_PASSAGE_NOT_FOUND";

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase;
    private final AiPromptVersionRepository promptVersionRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiGenerationJobRepository generationJobRepository;
    private final CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    private final Clock clock;

    AiDailyQtVerseExplanationSeedService(
            GetQtPassageContentContextUseCase getQtPassageContentContextUseCase,
            ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase,
            AiPromptVersionRepository promptVersionRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiGenerationJobRepository generationJobRepository,
            CreateAiGenerationJobUseCase createAiGenerationJobUseCase,
            Clock clock
    ) {
        this.getQtPassageContentContextUseCase = getQtPassageContentContextUseCase;
        this.listApprovedVerseExplanationUseCase = listApprovedVerseExplanationUseCase;
        this.promptVersionRepository = promptVersionRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.generationJobRepository = generationJobRepository;
        this.createAiGenerationJobUseCase = createAiGenerationJobUseCase;
        this.clock = clock;
    }

    /**
     * 오늘(KST) 본문의 해설 생성 job 시딩.
     *
     * <p>버그 수정(2026-06-05): 기존에는 사용자용 getToday()를 호출했는데
     * 그 API는 00:00~04:00에 "어제 본문"을 반환하는 노출 정책(STALE_FALLBACK)이라
     * 00:05 시딩이 항상 어제 본문을 시딩했다. 내부 배치 전용 날짜 직접 조회로 교체 (CLAUDE.md §6).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDailyQtVerseExplanationSeedResult seedToday() {
        LocalDate today = LocalDate.now(clock);
        QtPassageContentContext context = Objects.requireNonNull(
                getQtPassageContentContextUseCase.findContentContextByDate(today),
                "qtPassageContentContext optional must not be null"
        ).orElse(null);
        if (context == null) {
            log.warn("AI daily QT verse explanation seed skipped. reason={}, qtDate={}",
                    TODAY_PASSAGE_NOT_FOUND, today);
            return new AiDailyQtVerseExplanationSeedResult(0, 0, TODAY_PASSAGE_NOT_FOUND);
        }
        return seedForContext(context, SYSTEM_BATCH);
    }

    /**
     * 특정 QT 본문의 해설 생성 job 시딩 — 관리자 수동 트리거용(F-02/F-06).
     *
     * <p>오늘 본문에 한정하지 않고 {@code qtPassageId}로 직접 컨텍스트를 조회한다.
     * 노출 정책({@code getToday}의 STALE_FALLBACK)을 거치지 않으므로 관리자가 지정한
     * 본문 그대로 시딩한다. 이미 승인됐거나 진행 중인 절은 {@link #skippedVerseIds}로 건너뛴다.
     * 본문이 없으면 {@code getContentContext}가 도메인 예외를 던진다(상위에서 404 매핑).
     *
     * @param qtPassageId QT 본문 식별자
     * @param requestedBy 생성 job의 요청 주체(관리자 트리거 표기)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiDailyQtVerseExplanationSeedResult seedForPassage(Long qtPassageId, String requestedBy) {
        QtPassageContentContext context = getQtPassageContentContextUseCase
                .getContentContext(requirePositive(qtPassageId, "qtPassageId"));
        if (context == null) {
            return new AiDailyQtVerseExplanationSeedResult(0, 0, TODAY_PASSAGE_NOT_FOUND);
        }
        return seedForContext(context, requireRequestedBy(requestedBy));
    }

    /**
     * 컨텍스트(본문+절 목록) 기준 해설 생성 job 시딩 공통 로직.
     * {@link #seedToday()}(배치)와 {@link #seedForPassage}(관리자 트리거)가 공유한다.
     */
    private AiDailyQtVerseExplanationSeedResult seedForContext(QtPassageContentContext context, String requestedBy) {
        Long qtPassageId = requirePositive(context.qtPassageId(), "qtPassageId");
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
                        requestedBy,
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
                "AI daily QT verse explanation seed created jobs. qtPassageId={}, verseCount={}, skippedCount={}, createdCount={}, failedCount={}, requestedBy={}",
                qtPassageId,
                verseIds.size(),
                skippedVerseIds.size(),
                createdCount,
                failedCount,
                requestedBy
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

    private static String requireRequestedBy(String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "requestedBy must not be blank");
        }
        return requestedBy;
    }
}
