package com.qtai.bff.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 설정.
 *
 * <p>경로: WS /ws/notifications (HTTP 핸드셰이크는 익명 허용, STOMP CONNECT 헤더에서 JWT 검증).
 * <p>구독 토픽: /user/queue/notifications (Spring의 user-destination convertAndSendToUser).
 *
 * <p>TODO(강태오): {@link com.qtai.bff.websocket.StompAuthInterceptor}에서 CONNECT 시 JWT 검증.
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications").setAllowedOriginPatterns("*");
        // SockJS fallback은 필요시 .withSockJS()
    }
}
