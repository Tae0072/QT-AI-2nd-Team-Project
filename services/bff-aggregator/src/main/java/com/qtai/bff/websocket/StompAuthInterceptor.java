package com.qtai.bff.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 시 JWT 검증.
 *
 * <p>AGENTS.md / 04 § 10.2: WebSocket 인증은 HTTP 핸드셰이크 시점이 아니라
 * STOMP CONNECT 헤더의 Authorization로 처리한다.
 *
 * <p>TODO(강태오): NimbusJwtDecoder.decode(...) → Principal 부착.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // TODO: accessor.getNativeHeader("Authorization") 검증 → setUser(...)
        }
        return message;
    }
}
