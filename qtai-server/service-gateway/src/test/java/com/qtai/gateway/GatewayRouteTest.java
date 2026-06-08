package com.qtai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

/**
 * 라우트 정의 회귀 안전망 — `application.yml`의 monolith 라우트가 **Path 예측자 + CircuitBreaker
 * 필터**를 갖고 적재되는지 실제로 단언한다(잘못된/누락된 필터 설정을 PR에서 차단).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteTest {

    @Autowired
    RouteDefinitionLocator routeDefinitionLocator;

    @Test
    @DisplayName("monolith 라우트가 Path 예측자 + CircuitBreaker 필터로 적재된다")
    void monolithRouteHasCircuitBreakerFilterAndPathPredicate() {
        List<RouteDefinition> definitions =
                routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(definitions).isNotNull();
        RouteDefinition monolith = definitions.stream()
                .filter(d -> "monolith".equals(d.getId()))
                .findFirst()
                .orElse(null);

        assertThat(monolith).as("monolith 라우트 정의").isNotNull();
        assertThat(monolith.getPredicates())
                .extracting(PredicateDefinition::getName)
                .contains("Path");
        assertThat(monolith.getFilters())
                .extracting(FilterDefinition::getName)
                .contains("CircuitBreaker");
    }
}
