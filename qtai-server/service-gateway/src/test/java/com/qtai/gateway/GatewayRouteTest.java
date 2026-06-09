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

    @Test
    @DisplayName("bible-service 라우트가 Path 예측자 + CircuitBreaker + X-Gateway-Token 주입 필터로 적재된다")
    void bibleRouteHasCircuitBreakerAndGatewayTokenHeader() {
        RouteDefinition bible = routeDefinitions().stream()
                .filter(definition -> "bible-service".equals(definition.getId()))
                .findFirst()
                .orElse(null);

        assertThat(bible).as("bible-service 라우트 정의").isNotNull();
        assertThat(bible.getPredicates())
                .extracting(PredicateDefinition::getName)
                .contains("Path");
        assertThat(bible.getFilters())
                .extracting(FilterDefinition::getName)
                .contains("CircuitBreaker", "AddRequestHeader");
    }

    @Test
    @DisplayName("bible-service 라우트가 monolith catch-all 보다 앞 순서다(순서 회귀 방지)")
    void bibleRouteIsOrderedBeforeMonolithCatchAll() {
        List<String> ids = routeDefinitions().stream().map(RouteDefinition::getId).toList();

        // Spring Cloud Gateway는 순차 평가 → bible 라우트가 catch-all(monolith)보다 먼저 와야 매칭된다.
        assertThat(ids).contains("bible-service", "monolith");
        assertThat(ids.indexOf("bible-service"))
                .as("bible-service 라우트가 monolith 보다 앞")
                .isLessThan(ids.indexOf("monolith"));
    }

    private List<RouteDefinition> routeDefinitions() {
        List<RouteDefinition> definitions =
                routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(definitions).isNotNull();
        return definitions;
    }
}
