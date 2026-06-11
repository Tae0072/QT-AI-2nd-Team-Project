package com.qtai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.qtai.gateway.config.AiServiceRouteConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "qtai.gateway.ai-service.route-enabled=true",
                "qtai.gateway.ai-service.uri=http://localhost:8081"
        })
class GatewayAiRouteEnabledTest {

    @Autowired
    RouteDefinitionLocator routeDefinitionLocator;

    @Test
    @DisplayName("ai-service cutover route is registered only for the fixed AI inbound paths")
    void registersAiServiceCutoverRoute() {
        List<RouteDefinition> definitions = routeDefinitions();
        RouteDefinition aiRoute = findRoute(definitions, AiServiceRouteConfiguration.ROUTE_ID);
        RouteDefinition monolith = findRoute(definitions, "monolith");

        assertThat(aiRoute.getUri().toString()).isEqualTo("http://localhost:8081");
        assertThat(aiRoute.getOrder()).isLessThan(monolith.getOrder());
        assertThat(aiRoute.getMetadata()).containsEntry("healthPath", "/actuator/health");

        PredicateDefinition pathPredicate = aiRoute.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .findFirst()
                .orElse(null);
        assertThat(pathPredicate).isNotNull();
        assertThat(pathPredicate.getArgs().values())
                .containsExactlyInAnyOrderElementsOf(AiServiceRouteConfiguration.AI_SERVICE_PATHS);

        FilterDefinition circuitBreaker = aiRoute.getFilters().stream()
                .filter(filter -> "CircuitBreaker".equals(filter.getName()))
                .findFirst()
                .orElse(null);
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getArgs())
                .containsEntry("name", AiServiceRouteConfiguration.CIRCUIT_BREAKER_NAME)
                .containsEntry("fallbackUri", AiServiceRouteConfiguration.FALLBACK_URI);
    }

    private List<RouteDefinition> routeDefinitions() {
        List<RouteDefinition> definitions =
                routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(definitions).isNotNull();
        return definitions;
    }

    private RouteDefinition findRoute(List<RouteDefinition> definitions, String routeId) {
        return definitions.stream()
                .filter(definition -> routeId.equals(definition.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Route not found: " + routeId));
    }
}
