package com.qtai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * 라우트 적재 회귀 안전망 — `application.yml`의 route 정의(예측자·필터 포함)가
 * 실제로 빌드되는지 검증한다. contextLoads보다 깊게, 잘못된 필터/예측자 설정을 잡는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    @DisplayName("monolith 라우트(CircuitBreaker 필터 포함)가 정상 적재된다")
    void monolithRouteIsRegistered() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).extracting(Route::getId).contains("monolith");
    }
}
