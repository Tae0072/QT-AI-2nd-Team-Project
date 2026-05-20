package com.qtai.domain.ai.api;

/**
 * AI 응답 생성 UseCase 포트.
 *
 * QT 묵상에 대한 AI 피드백을 생성한다. F-15 정책: 단발성 Q&A만 허용 —
 * SSE 스트리밍·세션형 대화 금지. 호출자(qt 도메인)가 결과를 받아 저장 책임을 진다.
 */
public interface GenerateAiResponseUseCase {

    // TODO: AiResponse generate(Long memberId, AiPromptRequest request);
    //       단발 호출, 동기 반환. 내부에서 LlmClient.complete 호출 후 AiCallLog 기록.
}
