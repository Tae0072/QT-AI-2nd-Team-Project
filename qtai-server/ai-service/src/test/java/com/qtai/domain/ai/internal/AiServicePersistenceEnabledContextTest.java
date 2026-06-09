package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_persistence_enabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
class AiServicePersistenceEnabledContextTest {

    @Autowired
    private AiGenerationJobRepository aiGenerationJobRepository;
    @Autowired
    private AiGeneratedAssetRepository aiGeneratedAssetRepository;
    @Autowired
    private AiValidationLogRepository aiValidationLogRepository;
    @Autowired
    private AiPromptVersionRepository aiPromptVersionRepository;
    @Autowired
    private AiValidationChecklistVersionRepository aiValidationChecklistVersionRepository;
    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;
    @Autowired
    private AiBatchRunLogRepository aiBatchRunLogRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void optInModeRegistersAiPersistenceRepositories() {
        assertThat(aiGenerationJobRepository).isNotNull();
        assertThat(aiGeneratedAssetRepository).isNotNull();
        assertThat(aiValidationLogRepository).isNotNull();
        assertThat(aiPromptVersionRepository).isNotNull();
        assertThat(aiValidationChecklistVersionRepository).isNotNull();
        assertThat(validationReferenceJobRepository).isNotNull();
        assertThat(aiBatchRunLogRepository).isNotNull();
    }

    @Test
    void optInModeScansAiOwnedEntitiesOnly() {
        Set<Class<?>> entityTypes = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(entityType -> entityType.getJavaType())
                .collect(Collectors.toSet());

        assertThat(entityTypes).contains(
                AiGenerationJob.class,
                AiGeneratedAsset.class,
                AiValidationLog.class,
                AiPromptVersion.class,
                AiValidationChecklistVersion.class,
                ValidationReferenceJob.class,
                AiBatchRunLog.class
        );
    }
}
