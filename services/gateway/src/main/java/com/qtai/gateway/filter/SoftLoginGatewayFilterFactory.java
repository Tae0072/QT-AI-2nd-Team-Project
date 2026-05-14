package com.qtai.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * "Soft Login" 라우트용 필터.
 *
 * <p>DECISIONS.md §3에 따라 첫 진입은 비로그인 허용이지만, JWT가 헤더에 있으면
 * 그대로 다운스트림에 전달해 개인화 데이터를 제공한다.
 * 토큰이 잘못된 경우 401을 만들기보다는 anonymous로 떨어뜨린다.
 *
 * <p>TODO(강태오): 본 필터를 application.yml의 routes에 등록.
 */
@Component
public class SoftLoginGatewayFilterFactory extends AbstractGatewayFilterFactory<SoftLoginGatewayFilterFactory.Config> {

    public SoftLoginGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                // anonymous request: 다운스트림이 알아서 401을 결정한다
            }
            // 통과만 한다. 실제 검증은 ResourceServer가 수행.
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // 향후 라우트별 옵션 (예: rate limit key)을 위해 자리만 둠
    }
}
