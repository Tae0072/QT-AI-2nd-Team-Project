package com.qtai.domain.ai.web;

/**
 * AI REST 엔드포인트. base path: /api/v1/ai
 *
 * F-15 정책: 단발 요청 → 동기 응답 또는 202 Accepted + polling 패턴만 허용.
 * SSE / WebSocket 스트리밍 엔드포인트 금지.
 *
 * 엔드포인트:
 *   POST /generate  → AI 응답 생성 (GenerateAiResponseUseCase)
 */
// TODO: @RestController, @RequestMapping("/api/v1/ai"), @RequiredArgsConstructor
public class AiController {

    // TODO: final GenerateAiResponseUseCase generateAiResponseUseCase;

    // TODO: POST "/generate" — @RequestBody AiPromptRequest + @AuthenticationPrincipal memberId
    //       → ApiResponse.success(AiResponse) 반환
}
