package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.domain.ai.web.AdminAiAssetController;
import com.qtai.domain.ai.web.AdminAiBatchRunLogController;
import com.qtai.domain.ai.web.AdminAiMonitoringController;
import com.qtai.domain.ai.web.AdminAiValidationChecklistController;
import com.qtai.domain.ai.web.SystemAiAssetController;
import com.qtai.domain.ai.web.SystemAiGenerationJobController;
import com.qtai.domain.ai.web.SystemAiValidationLogController;
import com.qtai.domain.ai.web.SystemValidationReferenceJobController;

@SpringBootTest(classes = AiServiceApplication.class)
@ActiveProfiles("test")
class AiServiceInboundDisabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void defaultModeDoesNotRegisterInboundControllers() {
        assertThat(context.getBeansOfType(SystemAiGenerationJobController.class)).isEmpty();
        assertThat(context.getBeansOfType(SystemAiAssetController.class)).isEmpty();
        assertThat(context.getBeansOfType(SystemAiValidationLogController.class)).isEmpty();
        assertThat(context.getBeansOfType(SystemValidationReferenceJobController.class)).isEmpty();
        assertThat(context.getBeansOfType(AdminAiAssetController.class)).isEmpty();
        assertThat(context.getBeansOfType(AdminAiMonitoringController.class)).isEmpty();
        assertThat(context.getBeansOfType(AdminAiBatchRunLogController.class)).isEmpty();
        assertThat(context.getBeansOfType(AdminAiValidationChecklistController.class)).isEmpty();
    }
}
