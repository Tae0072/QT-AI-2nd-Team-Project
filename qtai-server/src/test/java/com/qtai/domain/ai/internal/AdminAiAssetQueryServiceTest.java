package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

class AdminAiAssetQueryServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-21T10:30:00+09:00");

    private AdminAiAssetQueryRepository repository;
    private AdminAiAssetQueryService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AdminAiAssetQueryRepository.class);
        service = new AdminAiAssetQueryService(repository, new ObjectMapper());
    }

    @Test
    void serviceImplementsAdminQueryUseCases() {
        assertThat(service).isInstanceOf(ListAdminAiAssetsUseCase.class);
        assertThat(service).isInstanceOf(GetAdminAiAssetUseCase.class);
    }

    @Test
    void reviewerAndSuperAdminCanListAssets() {
        when(repository.findAll(any(ListAdminAiAssetsQuery.class), any(Pageable.class)))
                .thenReturn(new AdminAiAssetQueryRepository.AdminAiAssetPage(List.of(), 0L));

        service.listAdminAiAssets(listQuery("REVIEWER"));
        service.listAdminAiAssets(listQuery("SUPER_ADMIN"));

        ArgumentCaptor<ListAdminAiAssetsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListAdminAiAssetsQuery.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, org.mockito.Mockito.times(2)).findAll(queryCaptor.capture(), pageableCaptor.capture());
        assertThat(queryCaptor.getAllValues())
                .extracting(ListAdminAiAssetsQuery::adminRole)
                .containsExactly("REVIEWER", "SUPER_ADMIN");
        assertThat(pageableCaptor.getAllValues())
                .allSatisfy(pageable -> {
                    assertThat(pageable.getPageNumber()).isZero();
                    assertThat(pageable.getPageSize()).isEqualTo(20);
                    assertThat(pageable.getSort().toString()).isEqualTo("createdAt: DESC");
                });
    }

    @Test
    void operatorOrNonAdminRoleCannotListAssets() {
        assertForbidden(() -> service.listAdminAiAssets(listQuery("OPERATOR")));
        assertForbidden(() -> service.listAdminAiAssets(new ListAdminAiAssetsQuery(
                7L,
                "USER",
                "REVIEWER",
                null,
                null,
                null,
                null,
                null,
                0,
                20
        )));
    }

    @Test
    void listAssetsMapsRepositoryRowsToPageResponseWithSort() {
        when(repository.findAll(any(ListAdminAiAssetsQuery.class), any(Pageable.class)))
                .thenReturn(new AdminAiAssetQueryRepository.AdminAiAssetPage(
                        List.of(new AdminAiAssetQueryRepository.AdminAiAssetListRow(
                                500L,
                                AiGeneratedAssetType.EXPLANATION,
                                AiTargetType.QT_PASSAGE,
                                100L,
                                AiGeneratedAssetStatus.VALIDATING,
                                "QT-AI 검토용 출처",
                                CREATED_AT,
                                3L,
                                AiPromptType.EXPLANATION,
                                "2026.05.1",
                                AiPromptVersionStatus.ACTIVE,
                                AiValidationResult.NEEDS_REVIEW,
                                12L
                        )),
                        21L
                ));

        AdminAiAssetListResponse response = service.listAdminAiAssets(new ListAdminAiAssetsQuery(
                7L,
                "ADMIN",
                "REVIEWER",
                "EXPLANATION",
                "QT_PASSAGE",
                "VALIDATING",
                3L,
                12L,
                1,
                10
        ));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).promptVersion().id()).isEqualTo(3L);
        assertThat(response.content().get(0).latestValidationResult()).isEqualTo("NEEDS_REVIEW");
        assertThat(response.content().get(0).sourceLabelPresent()).isTrue();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(21L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.sort()).isEqualTo("createdAt,desc");
    }

    @Test
    void getAssetThrowsAiAssetNotFoundWhenRepositoryHasNoDetail() {
        when(repository.findDetail(500L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAdminAiAsset(detailQuery("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_ASSET_NOT_FOUND));
    }

    @Test
    void getAssetReturnsStoredPayloadWithoutAddingForbiddenRawFields() {
        when(repository.findDetail(500L)).thenReturn(Optional.of(detailRow()));
        when(repository.findValidationLogs(500L)).thenReturn(List.of(new AdminAiAssetQueryRepository.AdminAiValidationLogRow(
                900L,
                300L,
                12L,
                2,
                AiValidationResult.NEEDS_REVIEW,
                AiValidationReviewerType.ADMIN,
                "출처 표시 확인 필요",
                CREATED_AT.plusMinutes(10)
        )));

        AdminAiAssetDetailResponse response = service.getAdminAiAsset(detailQuery("SUPER_ADMIN"));

        assertThat(response.payloadJson().get("summary").asText()).isEqualTo("검토용 요약");
        assertThat(response.payloadJson().has("providerRawResponse")).isFalse();
        assertThat(response.payloadJson().has("rawResponse")).isFalse();
        assertThat(response.payloadJson().has("validationReferenceText")).isFalse();
        assertThat(response.payloadJson().has("sec" + "ret")).isFalse();
        assertThat(response.payloadJson().has("to" + "ken")).isFalse();
        assertThat(response.payloadJson().has("pass" + "word")).isFalse();
        assertThat(response.validationLogs())
                .hasSize(1)
                .first()
                .satisfies(log -> {
                    assertThat(log.validationLogId()).isEqualTo(900L);
                    assertThat(log.checklistVersionId()).isEqualTo(12L);
                    assertThat(log.result()).isEqualTo("NEEDS_REVIEW");
                });
    }

    private static ListAdminAiAssetsQuery listQuery(String adminRole) {
        return new ListAdminAiAssetsQuery(
                7L,
                "ADMIN",
                adminRole,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        );
    }

    private static GetAdminAiAssetQuery detailQuery(String adminRole) {
        return new GetAdminAiAssetQuery(7L, "ADMIN", adminRole, 500L);
    }

    private static AdminAiAssetQueryRepository.AdminAiAssetDetailRow detailRow() {
        return new AdminAiAssetQueryRepository.AdminAiAssetDetailRow(
                500L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                100L,
                AiGeneratedAssetStatus.VALIDATING,
                "{\"summary\":\"검토용 요약\",\"sourceLabel\":\"QT-AI 검토용 출처\"}",
                "QT-AI 검토용 출처",
                CREATED_AT,
                null,
                101L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                100L,
                3L,
                AiGenerationJobStatus.SUCCEEDED,
                CREATED_AT.minusMinutes(30),
                CREATED_AT.minusMinutes(20),
                CREATED_AT.minusMinutes(10),
                null,
                3L,
                AiPromptType.EXPLANATION,
                "2026.05.1",
                AiPromptVersionStatus.ACTIVE
        );
    }

    private static void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
