package com.qtai.gateway.config;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiServiceRouteProperties.class)
public class AiServiceRouteConfiguration {

    public static final String ROUTE_ID = "ai-service-cutover";
    public static final String CIRCUIT_BREAKER_NAME = "aiServiceCb";
    public static final String FALLBACK_URI = "forward:/__fallback";
    public static final List<String> AI_SERVICE_PATHS = List.of(
            "/api/v1/system/ai/**",
            "/api/v1/system/validation-reference-jobs/**",
            "/api/v1/admin/ai/**");

    @Bean
    @ConditionalOnProperty(prefix = "qtai.gateway.ai-service", name = "route-enabled", havingValue = "true")
    RouteDefinitionLocator aiServiceRouteDefinitionLocator(AiServiceRouteProperties properties) {
        if (!StringUtils.hasText(properties.getUri())) {
            throw new IllegalStateException(
                    "qtai.gateway.ai-service.uri must be configured when ai-service route is enabled");
        }

        RouteDefinition definition = new RouteDefinition();
        definition.setId(ROUTE_ID);
        definition.setUri(URI.create(properties.getUri()));
        definition.setOrder(properties.getRouteOrder());
        definition.setPredicates(List.of(pathPredicate()));
        definition.setFilters(List.of(circuitBreakerFilter()));
        definition.setMetadata(Map.of("healthPath", properties.getHealthPath()));
        return () -> Flux.just(definition);
    }

    private static PredicateDefinition pathPredicate() {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        for (int i = 0; i < AI_SERVICE_PATHS.size(); i++) {
            predicate.addArg("_genkey_" + i, AI_SERVICE_PATHS.get(i));
        }
        return predicate;
    }

    private static FilterDefinition circuitBreakerFilter() {
        FilterDefinition filter = new FilterDefinition();
        filter.setName("CircuitBreaker");
        filter.addArg("name", CIRCUIT_BREAKER_NAME);
        filter.addArg("fallbackUri", FALLBACK_URI);
        return filter;
    }
}
