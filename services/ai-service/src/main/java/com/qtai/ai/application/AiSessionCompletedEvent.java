package com.qtai.ai.application;

import com.qtai.ai.domain.AiSession;

/** ai.session.completed Kafka 발행을 트리거하는 애플리케이션 이벤트. */
public record AiSessionCompletedEvent(AiSession session) {}
