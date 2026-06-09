package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;

@SpringBootTest(classes = AiServiceApplication.class)
@ActiveProfiles("test")
class AiServicePersistenceDisabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void defaultModeDoesNotRegisterAiPersistenceRepositories() {
        assertThat(context.getBeansOfType(AiGenerationJobRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(AiGeneratedAssetRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(AiValidationLogRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(AiPromptVersionRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(AiValidationChecklistVersionRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(ValidationReferenceJobRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(AiBatchRunLogRepository.class)).isEmpty();
    }
}
