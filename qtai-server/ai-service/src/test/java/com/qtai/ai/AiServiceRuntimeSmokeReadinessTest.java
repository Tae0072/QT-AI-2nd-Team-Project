package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.qtai.domain.ai.client.admin.AdminAuthClientMock;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;
import com.qtai.domain.ai.client.bible.BibleVerseClientMock;
import com.qtai.domain.ai.client.qt.GetQtUseCaseMock;
import com.qtai.domain.ai.client.study.StudyPublishClientMock;

@SpringBootTest(
        classes = AiServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "qtai.ai.inbound.enabled=true",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.client.mode=mock",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_runtime_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiServiceRuntimeSmokeReadinessTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void startsAsStandaloneRuntimeWithHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void registersAiInboundMappingsForFutureGatewayCutover() {
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

    @Test
    void usesMockOutboundClientsInRuntimeSmokeMode() {
        assertThat(context.getBeansOfType(GetQtUseCaseMock.class)).hasSize(1);
        assertThat(context.getBeansOfType(BibleVerseClientMock.class)).hasSize(1);
        assertThat(context.getBeansOfType(StudyPublishClientMock.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuditLogClientMock.class)).hasSize(1);
        assertThat(context.getBeansOfType(AdminAuthClientMock.class)).hasSize(1);
    }

    private Set<String> mappingPaths() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(AiServiceRuntimeSmokeReadinessTest::patterns)
                .collect(Collectors.toSet());
    }

    private static Stream<String> patterns(RequestMappingInfo info) {
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
