package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

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
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_migration_validation;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=validate",
                "qtai.ai.persistence.dialect=org.hibernate.dialect.MySQLDialect",
                "qtai.ai.persistence.flyway-enabled=true"
        }
)
@ActiveProfiles("test")
class AiServiceMigrationValidationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void flywayCreatesAiOwnedTablesAndHibernateValidatePasses() throws SQLException {
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(entityType -> entityType.getJavaType().getSimpleName())
                .contains(
                        "AiGenerationJob",
                        "AiGeneratedAsset",
                        "AiValidationLog",
                        "AiPromptVersion",
                        "AiValidationChecklistVersion",
                        "ValidationReferenceJob",
                        "AiBatchRunLog",
                        "AiEventOutbox",
                        "AiProcessedEvent"
                );

        assertThat(tableNames()).contains(
                "ai_generation_jobs",
                "ai_generated_assets",
                "ai_validation_logs",
                "ai_prompt_versions",
                "ai_validation_checklist_versions",
                "validation_reference_jobs",
                "ai_batch_run_logs",
                "ai_event_outbox",
                "ai_processed_events",
                "flyway_schema_history"
        );
    }

    private Set<String> tableNames() throws SQLException {
        Set<String> result = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[] {"TABLE"})) {
            while (tables.next()) {
                result.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }
}
