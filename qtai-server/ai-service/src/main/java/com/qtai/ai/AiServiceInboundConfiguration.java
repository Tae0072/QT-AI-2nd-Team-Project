package com.qtai.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.domain.ai.web.AdminAiAssetController;
import com.qtai.domain.ai.web.AdminAiAuthentication;
import com.qtai.domain.ai.web.AdminAiBatchRunLogController;
import com.qtai.domain.ai.web.AdminAiMonitoringController;
import com.qtai.domain.ai.web.AdminAiValidationChecklistController;
import com.qtai.domain.ai.web.SystemAiAssetController;
import com.qtai.domain.ai.web.SystemAiGenerationJobController;
import com.qtai.domain.ai.web.SystemAiValidationLogController;
import com.qtai.domain.ai.web.SystemValidationReferenceJobController;

@Configuration
@ConditionalOnProperty(name = "qtai.ai.inbound.enabled", havingValue = "true")
@Import({
        AdminAiAuthentication.class,
        SystemAiGenerationJobController.class,
        SystemAiAssetController.class,
        SystemAiValidationLogController.class,
        SystemValidationReferenceJobController.class,
        AdminAiAssetController.class,
        AdminAiMonitoringController.class,
        AdminAiBatchRunLogController.class,
        AdminAiValidationChecklistController.class
})
public class AiServiceInboundConfiguration {
}
