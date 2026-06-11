package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.checklist.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.admin.checklist.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.web.AdminAiAssetController;
import com.qtai.domain.ai.web.AdminAiBatchRunLogController;
import com.qtai.domain.ai.web.AdminAiMonitoringController;
import com.qtai.domain.ai.web.AdminAiValidationChecklistController;
import com.qtai.domain.ai.web.SystemAiAssetController;
import com.qtai.domain.ai.web.SystemAiGenerationJobController;
import com.qtai.domain.ai.web.SystemAiValidationLogController;
import com.qtai.domain.ai.web.SystemValidationReferenceJobController;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = "qtai.ai.inbound.enabled=true"
)
@ActiveProfiles("test")
class AiServiceInboundEnabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    @MockBean
    private RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase;
    @MockBean
    private RegisterAiValidationLogUseCase registerAiValidationLogUseCase;
    @MockBean
    private CreateValidationReferenceJobUseCase createValidationReferenceJobUseCase;
    @MockBean
    private GetValidationReferenceJobUseCase getValidationReferenceJobUseCase;
    @MockBean
    private ExpireValidationReferenceJobUseCase expireValidationReferenceJobUseCase;
    @MockBean
    private ListAdminAiAssetsUseCase listAdminAiAssetsUseCase;
    @MockBean
    private GetAdminAiAssetUseCase getAdminAiAssetUseCase;
    @MockBean
    private ReviewAiAssetUseCase reviewAiAssetUseCase;
    @MockBean
    private RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    @MockBean
    private GetAdminAiMonitoringUseCase getAdminAiMonitoringUseCase;
    @MockBean
    private ListAdminAiBatchRunLogsUseCase listAdminAiBatchRunLogsUseCase;
    @MockBean
    private ListAdminAiValidationChecklistsUseCase listAdminAiValidationChecklistsUseCase;
    @MockBean
    private CreateAdminAiValidationChecklistUseCase createAdminAiValidationChecklistUseCase;
    @MockBean
    private ActivateAdminAiValidationChecklistUseCase activateAdminAiValidationChecklistUseCase;
    @MockBean
    private RetireAdminAiValidationChecklistUseCase retireAdminAiValidationChecklistUseCase;

    @Test
    void optInModeRegistersInboundControllersAndMappings() {
        assertThat(context.getBean(SystemAiGenerationJobController.class)).isNotNull();
        assertThat(context.getBean(SystemAiAssetController.class)).isNotNull();
        assertThat(context.getBean(SystemAiValidationLogController.class)).isNotNull();
        assertThat(context.getBean(SystemValidationReferenceJobController.class)).isNotNull();
        assertThat(context.getBean(AdminAiAssetController.class)).isNotNull();
        assertThat(context.getBean(AdminAiMonitoringController.class)).isNotNull();
        assertThat(context.getBean(AdminAiBatchRunLogController.class)).isNotNull();
        assertThat(context.getBean(AdminAiValidationChecklistController.class)).isNotNull();

        assertThat(mappingPaths()).contains(
                "/api/v1/system/ai/generation-jobs",
                "/api/v1/system/ai/assets",
                "/api/v1/system/ai/validation-logs",
                "/api/v1/system/validation-reference-jobs",
                "/api/v1/system/validation-reference-jobs/{jobId}",
                "/api/v1/system/validation-reference-jobs/{jobId}/expire",
                "/api/v1/admin/ai/assets",
                "/api/v1/admin/ai/assets/{assetId}",
                "/api/v1/admin/ai/assets/{assetId}/approve",
                "/api/v1/admin/ai/assets/{assetId}/reject",
                "/api/v1/admin/ai/assets/{assetId}/hide",
                "/api/v1/admin/ai/assets/{assetId}/regenerate",
                "/api/v1/admin/ai/monitoring",
                "/api/v1/admin/ai/batch-run-logs",
                "/api/v1/admin/ai/validation-checklists",
                "/api/v1/admin/ai/validation-checklists/{id}/activate",
                "/api/v1/admin/ai/validation-checklists/{id}/retire"
        );
    }

    private Set<String> mappingPaths() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(this::patterns)
                .collect(Collectors.toSet());
    }

    private Stream<String> patterns(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatterns().stream()
                    .map(Object::toString);
        }
        if (info.getPatternsCondition() != null) {
            return info.getPatternsCondition().getPatterns().stream();
        }
        return Stream.empty();
    }
}
