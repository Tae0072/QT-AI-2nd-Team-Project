package com.qtai.domain.ai.internal;

/**
 * AI 도메인 진입점. GenerateAiResponseUseCase 구현 + 트랜잭션 경계.
 *
 * 외부 호출:
 *   - external.llm.LlmClient (provider-agnostic 포트) — 직접 주입
 *   - qt 도메인 컨텍스트는 client/ 어댑터(GetQtUseCase)로만 접근
 *
 * 정책:
 *   - F-15: 단발성 Q&A만 (SSE/세션형 금지)
 *   - DeepSeek API만 허용 (Anthropic SDK 직접 사용 금지)
 *   - 호출마다 AiCallLog 기록 (content 본문은 저장 X)
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements GenerateAiResponseUseCase
public class AiService {

    // TODO: final LlmClient llmClient;
    // TODO: final GetQtUseCase getQtUseCase;  (client/qt 어댑터)
    // TODO: final AiCallLogRepository aiCallLogRepository;

    // TODO: @Transactional generate(memberId, request) 구현
    //       1) qtId 있으면 getQtUseCase.getQt(...) 로 컨텍스트 조회
    //       2) systemPrompt + 컨텍스트 + userPrompt 조립
    //       3) llmClient.complete(...) 호출
    //       4) AiCallLog 저장 (토큰 사용량만, content 제외)
    //       5) AiResponse 반환
}
