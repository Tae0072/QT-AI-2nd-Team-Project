package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
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
import com.qtai.domain.ai.web.SystemAiGenerationJobController;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.inbound.enabled=true",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_usecase_context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiServiceUseCasePersistenceContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void optInModeRegistersControllersAndRealUseCaseBeans() {
        assertThat(context.getBean(SystemAiGenerationJobController.class)).isNotNull();
        assertThat(context.getBean(AdminAiAssetController.class)).isNotNull();

        assertThat(context.getBeansOfType(CreateAiGenerationJobUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(RegisterAiGeneratedAssetUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(RegisterAiValidationLogUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(CreateValidationReferenceJobUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(GetValidationReferenceJobUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ExpireValidationReferenceJobUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ListAdminAiAssetsUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(GetAdminAiAssetUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ReviewAiAssetUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(RegenerateAiAssetUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(GetAdminAiMonitoringUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ListAdminAiBatchRunLogsUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ListAdminAiValidationChecklistsUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(CreateAdminAiValidationChecklistUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(ActivateAdminAiValidationChecklistUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(RetireAdminAiValidationChecklistUseCase.class)).hasSize(1);
    }
}
