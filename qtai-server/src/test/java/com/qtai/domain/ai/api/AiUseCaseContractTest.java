package com.qtai.domain.ai.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogItem;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.checklist.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.admin.checklist.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListItem;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiValidationLogItem;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.admin.checklist.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;
import com.qtai.domain.ai.api.qa.dto.GetAiQaResultCommand;
import com.qtai.domain.ai.api.qa.dto.GetAiQaResultResult;
import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.admin.checklist.dto.ListAdminAiValidationChecklistsQuery;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetResult;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogResult;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetResult;
import com.qtai.domain.ai.api.qa.GetAiQaResultUseCase;
import com.qtai.domain.ai.api.qa.RequestAiQaUseCase;
import com.qtai.domain.ai.api.qa.dto.RequestAiQaCommand;
import com.qtai.domain.ai.api.qa.dto.RequestAiQaResult;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

class AiUseCaseContractTest {

    private static final List<Class<?>> USE_CASES = List.of(
            RequestAiQaUseCase.class,
            GetAiQaResultUseCase.class,
            CreateAiGenerationJobUseCase.class,
            RegisterAiGeneratedAssetUseCase.class,
            RegisterAiValidationLogUseCase.class,
            ReviewAiAssetUseCase.class,
            RegenerateAiAssetUseCase.class,
            ListAdminAiAssetsUseCase.class,
            GetAdminAiAssetUseCase.class,
            ListAdminAiValidationChecklistsUseCase.class,
            CreateAdminAiValidationChecklistUseCase.class,
            ActivateAdminAiValidationChecklistUseCase.class,
            RetireAdminAiValidationChecklistUseCase.class,
            ListAdminAiBatchRunLogsUseCase.class,
            GetAdminAiMonitoringUseCase.class,
            CreateValidationReferenceJobUseCase.class,
            GetValidationReferenceJobUseCase.class,
            ExpireValidationReferenceJobUseCase.class
    );

    private static final List<Class<?>> USE_CASE_DTOS = List.of(
            RequestAiQaCommand.class,
            RequestAiQaResult.class,
            GetAiQaResultCommand.class,
            GetAiQaResultResult.class,
            CreateAiGenerationJobCommand.class,
            CreateAiGenerationJobResult.class,
            RegisterAiGeneratedAssetCommand.class,
            RegisterAiGeneratedAssetResult.class,
            RegisterAiValidationLogCommand.class,
            RegisterAiValidationLogResult.class,
            ReviewAiAssetCommand.class,
            ReviewAiAssetResult.class,
            RegenerateAiAssetCommand.class,
            RegenerateAiAssetResult.class,
            ListAdminAiAssetsQuery.class,
            GetAdminAiAssetQuery.class,
            AdminAiAssetListResponse.class,
            AdminAiAssetListItem.class,
            AdminAiAssetDetailResponse.class,
            AdminAiValidationLogItem.class,
            ListAdminAiValidationChecklistsQuery.class,
            CreateAdminAiValidationChecklistCommand.class,
            ChangeAdminAiValidationChecklistStatusCommand.class,
            AdminAiValidationChecklistResponse.class,
            AdminAiValidationChecklistListResponse.class,
            ListAdminAiBatchRunLogsQuery.class,
            AdminAiBatchRunLogItem.class,
            AdminAiBatchRunLogListResponse.class,
            GetAdminAiMonitoringQuery.class,
            AdminAiMonitoringResponse.class,
            CreateValidationReferenceJobCommand.class,
            GetValidationReferenceJobQuery.class,
            ExpireValidationReferenceJobCommand.class,
            ValidationReferenceJobResponse.class
    );

    @Test
    void publicAiContractsAreUseCaseInterfaces() {
        assertThat(USE_CASES)
                .allSatisfy(useCase -> {
                    assertThat(useCase).isInterface();
                    assertThat(useCase.getSimpleName()).endsWith("UseCase");
                });
    }

    @Test
    void useCaseMethodsUseCommandAndResultRecordsOnly() {
        for (Class<?> useCase : USE_CASES) {
            Method[] methods = useCase.getDeclaredMethods();

            assertThat(methods).hasSize(1);
            assertThat(methods[0].getParameterTypes())
                    .hasSize(1)
                    .allSatisfy(parameterType ->
                            assertThat(parameterType.getSimpleName()).matches(".*(Command|Query)$"));
            assertThat(methods[0].getReturnType().getSimpleName()).matches(".*(Result|Response)$");
        }
    }

    @Test
    void useCaseDtosAreCommandOrResultRecords() {
        assertThat(USE_CASE_DTOS)
                .allSatisfy(dto -> {
                    assertThat(dto.isRecord()).isTrue();
                    assertThat(dto.getPackageName())
                            .startsWith("com.qtai.domain.ai.api.")
                            .endsWith(".dto");
                    assertThat(dto.getSimpleName()).matches(".*(Command|Query|Result|Response|Item)$");
                });
    }

    @Test
    void useCaseDtosDoNotExposeRawProviderOrValidationReferenceText() {
        assertThat(USE_CASE_DTOS)
                .allSatisfy(dto -> assertThat(List.of(dto.getRecordComponents()))
                        .extracting(RecordComponent::getName)
                        .doesNotContain(
                                "providerRawResponse",
                                "rawResponse",
                                "validationReferenceText",
                                "referenceText",
                                "promptRaw"
                        ));
    }

    @Test
    void validationReferenceJobResponseDoesNotExposeRestrictedLocationOrHash() {
        assertThat(List.of(ValidationReferenceJobResponse.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("id", "sourceName", "sourceFileName", "status", "expiresAt", "deletedAt", "createdAt", "updatedAt")
                .doesNotContain("sourceFileHash", "storageUri", "indexStorageUri");
    }

    @Test
    void registerAiValidationLogCommandIncludesNullableValidationReferenceJobId() {
        assertThat(List.of(RegisterAiValidationLogCommand.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("validationReferenceJobId");
    }

    @Test
    void adminAiAssetQueryDtosIncludeAdminAuthorizationContext() {
        assertThat(List.of(ListAdminAiAssetsQuery.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole");
        assertThat(List.of(GetAdminAiAssetQuery.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "assetId");
        assertThat(List.of(ReviewAiAssetCommand.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains(
                        "reviewerId",
                        "assetId",
                        "memberRole",
                        "adminRole",
                        "action",
                        "checklistVersionId",
                        "reason",
                        "activateForTarget",
                        "reviewedAt"
                );
    }

    @Test
    void adminAiValidationChecklistDtosIncludeAdminAuthorizationContext() {
        assertThat(List.of(ListAdminAiValidationChecklistsQuery.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "checklistType", "status", "page", "size");
        assertThat(List.of(CreateAdminAiValidationChecklistCommand.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "checklistType", "version", "contentHash", "status");
        assertThat(List.of(ChangeAdminAiValidationChecklistStatusCommand.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "checklistId");
    }

    @Test
    void adminAiBatchRunLogDtosIncludeAdminAuthorizationContextAndFilters() {
        assertThat(List.of(ListAdminAiBatchRunLogsQuery.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "batchName", "status", "from", "to", "page", "size");
        assertThat(List.of(AdminAiBatchRunLogItem.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains(
                        "id",
                        "batchName",
                        "status",
                        "createdCount",
                        "failedCount",
                        "processedCount",
                        "errorType",
                        "errorMessage",
                        "startedAt",
                        "finishedAt",
                        "createdAt"
                );
        assertThat(List.of(AdminAiBatchRunLogListResponse.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("content", "page", "size", "totalElements", "totalPages", "first", "last", "sort");
    }

    @Test
    void adminAiMonitoringDtosIncludeAdminAuthorizationContextAndSummarySections() {
        assertThat(List.of(GetAdminAiMonitoringQuery.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("adminId", "memberRole", "adminRole", "from", "to");
        assertThat(List.of(AdminAiMonitoringResponse.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .contains("period", "generationJobs", "validation", "batchRuns", "qa", "checklists");
    }

    @Test
    void legacyFreeFormAiResponseContractIsRemoved() {
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.GenerateAiResponseUseCase"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.dto.AiPromptRequest"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.qtai.domain.ai.api.dto.AiResponse"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
