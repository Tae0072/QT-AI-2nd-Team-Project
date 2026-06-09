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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteTest {

    @Autowired
    RouteDefinitionLocator routeDefinitionLocator;

    @Test
    @DisplayName("monolith route keeps Path predicate and CircuitBreaker filter")
    void monolithRouteHasCircuitBreakerFilterAndPathPredicate() {
        RouteDefinition monolith = routeDefinitions().stream()
                .filter(definition -> "monolith".equals(definition.getId()))
                .findFirst()
                .orElse(null);

        assertThat(monolith).as("monolith route definition").isNotNull();
        assertThat(monolith.getPredicates())
                .extracting(PredicateDefinition::getName)
                .contains("Path");
        assertThat(monolith.getFilters())
                .extracting(FilterDefinition::getName)
                .contains("CircuitBreaker");
    }

    @Test
    @DisplayName("default config does not register ai-service cutover route")
    void defaultConfigDoesNotRegisterAiServiceRoute() {
        assertThat(routeDefinitions())
                .extracting(RouteDefinition::getId)
                .doesNotContain(AiServiceRouteConfiguration.ROUTE_ID);
    }

    private List<RouteDefinition> routeDefinitions() {
        List<RouteDefinition> definitions =
                routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(definitions).isNotNull();
        return definitions;
    }
}
