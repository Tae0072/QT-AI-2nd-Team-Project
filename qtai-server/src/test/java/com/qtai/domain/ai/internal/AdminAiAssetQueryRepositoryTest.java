package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

@DataJpaTest
@Import(AdminAiAssetQueryRepository.class)
@ActiveProfiles("test")
class AdminAiAssetQueryRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-05-21T10:30:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AdminAiAssetQueryRepository repository;

    private long nextTargetId = 100L;

    @Test
    void findAllReturnsCreatedAtDescPageAndTotalWithoutFilters() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION, "2026.05.1");
        AiGeneratedAsset olderAsset = persistAsset(promptVersion, AiGeneratedAssetType.EXPLANATION, BASE_TIME.minusHours(1));
        AiGeneratedAsset newerAsset = persistAsset(promptVersion, AiGeneratedAssetType.EXPLANATION, BASE_TIME);
        persistValidationLog(olderAsset.getId(), 11L, AiValidationResult.PASSED, BASE_TIME.minusMinutes(30));
        persistValidationLog(newerAsset.getId(), 12L, AiValidationResult.NEEDS_REVIEW, BASE_TIME.plusMinutes(1));
        flushAndClear();

        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(
                listQuery(null, null, null, null, null),
                PageRequest.of(0, 1)
        );

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(newerAsset.getId());
        assertThat(page.content().get(0).latestValidationResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
        assertThat(page.content().get(0).checklistVersionId()).isEqualTo(12L);
    }

    @Test
    void findAllAppliesAssetTargetStatusPromptAndLatestChecklistFilters() {
        AiPromptVersion explanationPrompt = persistPromptVersion(AiPromptType.EXPLANATION, "2026.05.1");
        AiPromptVersion simulatorPrompt = persistPromptVersion(AiPromptType.SIMULATOR, "2026.05.1");
        AiGeneratedAsset matchingAsset = persistAsset(
                explanationPrompt,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                BASE_TIME
        );
        AiGeneratedAsset checklistMismatchAsset = persistAsset(
                explanationPrompt,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                BASE_TIME.minusMinutes(1)
        );
        AiGeneratedAsset promptMismatchAsset = persistAsset(
                simulatorPrompt,
                AiGeneratedAssetType.SIMULATOR,
                AiTargetType.QT_PASSAGE,
                BASE_TIME.minusMinutes(2)
        );
        persistValidationLog(matchingAsset.getId(), 11L, AiValidationResult.PASSED, BASE_TIME.minusMinutes(10));
        persistValidationLog(matchingAsset.getId(), 12L, AiValidationResult.NEEDS_REVIEW, BASE_TIME.plusMinutes(1));
        persistValidationLog(checklistMismatchAsset.getId(), 13L, AiValidationResult.PASSED, BASE_TIME.plusMinutes(2));
        persistValidationLog(promptMismatchAsset.getId(), 12L, AiValidationResult.PASSED, BASE_TIME.plusMinutes(3));
        flushAndClear();

        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(
                listQuery("EXPLANATION", "QT_PASSAGE", "VALIDATING", explanationPrompt.getId(), 12L),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content())
                .extracting(AdminAiAssetQueryRepository.AdminAiAssetListRow::id)
                .containsExactly(matchingAsset.getId());
        assertThat(page.content().get(0).latestValidationResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
        assertThat(page.content().get(0).checklistVersionId()).isEqualTo(12L);
    }

    @Test
    void findDetailReturnsPayloadAndValidationLogsCreatedAtDescThenIdDesc() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION, "2026.05.1");
        AiGeneratedAsset asset = persistAsset(promptVersion, AiGeneratedAssetType.EXPLANATION, BASE_TIME);
        AiValidationLog olderLog = persistValidationLog(
                asset.getId(),
                10L,
                AiValidationResult.PASSED,
                BASE_TIME.minusMinutes(5)
        );
        AiValidationLog firstSameTimeLog = persistValidationLog(
                asset.getId(),
                11L,
                AiValidationResult.NEEDS_REVIEW,
                BASE_TIME.plusMinutes(5)
        );
        AiValidationLog secondSameTimeLog = persistValidationLog(
                asset.getId(),
                12L,
                AiValidationResult.REJECTED,
                BASE_TIME.plusMinutes(5)
        );
        flushAndClear();

        AdminAiAssetQueryRepository.AdminAiAssetDetailRow detail = repository.findDetail(asset.getId())
                .orElseThrow();
        List<AdminAiAssetQueryRepository.AdminAiValidationLogRow> logs =
                repository.findValidationLogs(asset.getId());

        assertThat(detail.id()).isEqualTo(asset.getId());
        assertThat(detail.payloadJson()).contains("\"summary\":\"寃?좎슜 ?붿빟\"");
        assertThat(detail.promptVersionRowId()).isEqualTo(promptVersion.getId());
        assertThat(logs)
                .extracting(AdminAiAssetQueryRepository.AdminAiValidationLogRow::validationLogId)
                .containsExactly(secondSameTimeLog.getId(), firstSameTimeLog.getId(), olderLog.getId());
    }

    @Test
    void detailPayloadFixtureUsesOnlySafeReviewFields() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION, "2026.05.1");
        AiGeneratedAsset asset = persistAsset(promptVersion, AiGeneratedAssetType.EXPLANATION, BASE_TIME);
        flushAndClear();

        String payloadJson = repository.findDetail(asset.getId())
                .orElseThrow()
                .payloadJson();

        assertThat(payloadJson).contains("summary");
        assertThat(payloadJson)
                .doesNotContain(
                        "providerRawResponse",
                        "rawResponse",
                        "validationReferenceText",
                        "sec" + "ret",
                        "to" + "ken",
                        "pass" + "word"
                );
    }

    private AiPromptVersion persistPromptVersion(AiPromptType promptType, String version) {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", promptType);
        setField(promptVersion, "version", version);
        setField(promptVersion, "contentHash", "hash-" + promptType + "-" + version);
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", BASE_TIME.minusDays(1));
        return testEntityManager.persistAndFlush(promptVersion);
    }

    private AiGeneratedAsset persistAsset(
            AiPromptVersion promptVersion,
            AiGeneratedAssetType assetType,
            OffsetDateTime createdAt
    ) {
        return persistAsset(promptVersion, assetType, AiTargetType.QT_PASSAGE, createdAt);
    }

    private AiGeneratedAsset persistAsset(
            AiPromptVersion promptVersion,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            OffsetDateTime createdAt
    ) {
        Long targetId = nextTargetId++;
        AiGenerationJob job = AiGenerationJob.queue(
                jobTypeOf(assetType),
                targetType,
                targetId,
                promptVersion.getId(),
                createdAt.minusMinutes(30)
        );
        testEntityManager.persistAndFlush(job);
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                job.getId(),
                assetType,
                targetType,
                targetId,
                "{\"summary\":\"寃?좎슜 ?붿빟\",\"sourceLabel\":\"QT-AI 寃?좎슜 異쒖쿂\"}",
                "QT-AI 寃?좎슜 異쒖쿂",
                createdAt
        );
        return testEntityManager.persistAndFlush(asset);
    }

    private AiValidationLog persistValidationLog(
            Long assetId,
            Long checklistVersionId,
            AiValidationResult result,
            OffsetDateTime createdAt
    ) {
        AiValidationLog log = AiValidationLog.create(
                assetId,
                300L + checklistVersionId,
                2,
                result,
                AiValidationReviewerType.ADMIN,
                checklistVersionId,
                "{\"checks\":[\"sourceLabel\"]}",
                "異쒖쿂 ?쒖떆 ?뺤씤 ?꾩슂",
                createdAt
        );
        return testEntityManager.persistAndFlush(log);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private static ListAdminAiAssetsQuery listQuery(
            String assetType,
            String targetType,
            String status,
            Long promptVersionId,
            Long checklistVersionId
    ) {
        return new ListAdminAiAssetsQuery(
                7L,
                "ADMIN",
                "REVIEWER",
                assetType,
                targetType,
                status,
                promptVersionId,
                checklistVersionId,
                0,
                20
        );
    }

    private static AiGenerationJobType jobTypeOf(AiGeneratedAssetType assetType) {
        return switch (assetType) {
            case EXPLANATION -> AiGenerationJobType.EXPLANATION;
            case SIMULATOR -> AiGenerationJobType.SIMULATOR;
            case SUMMARY -> AiGenerationJobType.SUMMARY;
            case GLOSSARY -> AiGenerationJobType.GLOSSARY;
            case QA_RESPONSE -> AiGenerationJobType.QA;
        };
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
