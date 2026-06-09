package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.domain.ai.internal.AiGenerationWorkerService;

@SpringBootTest(classes = AiServiceApplication.class)
@ActiveProfiles("test")
class AiGenerationWorkerDisabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void defaultModeDoesNotRegisterGenerationWorker() {
        assertThat(context.getBeansOfType(AiGenerationWorkerService.class)).isEmpty();
    }
}
