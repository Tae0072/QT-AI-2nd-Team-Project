package com.qtai.bible;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.domain.bible.internal.BibleService;
import com.qtai.domain.bible.web.BibleController;

/**
 * bible-service inbound(HTTP) 구성. {@code qtai.bible.inbound.enabled=true}일 때만 활성화.
 *
 * <p>skeleton 단계는 비활성(false)이라 트래픽을 받지 않는다(게이트웨이는 여전히 모놀리식으로 라우팅).
 * 활성화 시 BibleController가 {@code /api/v1/bible/**}를 처리하며, 구현(BibleService)은 persistence
 * 활성화를 전제로 한다. 트래픽 컷오버는 게이트웨이 라우트 분기(Inc2)에서.
 *
 * <p>inbound와 함께 {@link GatewayHeaderAuthenticationFilter}를 등록해 게이트웨이 미경유 직접 호출을
 * deny-by-default로 차단한다(무인증 노출 방지). 필터가 inbound와 한 단위로 켜지므로 "켤 때 보안 누락"을 막는다.
 */
@Configuration
@ConditionalOnProperty(name = "qtai.bible.inbound.enabled", havingValue = "true")
@Import({
        BibleService.class,
        BibleController.class
})
public class BibleServiceInboundConfiguration {

    @Bean
    GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter(
            ObjectMapper objectMapper,
            @Value("${qtai.bible.gateway.shared-token:}") String gatewaySharedToken) {
        // 공유 토큰은 env 주입(저장소 평문 키 금지). 미설정이면 2차 방어선 비활성(헤더 필수 1단만).
        return new GatewayHeaderAuthenticationFilter(objectMapper, gatewaySharedToken);
    }
}
