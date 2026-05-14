# DevB 김태혁 — W1 첫 작업 가이드

## 담당 (DECISIONS.md §0 — 2026-05-14 재배치)
시뮬레이터 도메인 (스프라이트 기반 검토 중) + AI 서버 보조.
v2.0 NOTICE: `qtai-server/com.qtai.simulator/` 신설 예정 — 본 dev 레포에서는 `services/ai-service/` 보조부터 시작.

## 첫 PR 권장 순서

### 1. AI Service 부팅 + DeepSeek 호출 검증
- 위치: `services/ai-service/`
- 채워 넣을 곳:
  - `infrastructure/llm/DeepSeekStreamService.java` — base URL · 인증 헤더 · `choices[0].delta.content` 추출
  - `application/AiSessionService.streamTurn(...)` — emitter onTimeout / onError 시 Flux dispose
- 검증:
  ```bash
  curl -N -X POST http://localhost:8085/ai/sessions/1/turns \
       -H "Authorization: Bearer <JWT>" \
       -H "Content-Type: application/json" \
       -H "Accept: text/event-stream" \
       -d '{"userMessage":"이 구절의 의미를 알려주세요"}'
  ```

### 2. 시뮬레이터 도메인 스파이크
- `workspaces/DevB_김태혁/reports/spike_simulator.md`에 다음 문서화:
  - 스프라이트 vs 단순 텍스트 카드 비교
  - Flutter Sliver 활용 가능성
  - W4 분리 전까지 ai-service 또는 별도 모듈로 둘지 결정

## 금지
- Anthropic SDK / Claude 고정 코드 / `ANTHROPIC_API_KEY` 만들지 않기
- AI SSE 경로에 `/messages` 만들지 않기 — 정식 경로 `/ai/sessions/{id}/turns`
- RAG / ChromaDB / 벡터 DB 신규 사용 금지 (ADR-0013)

## 산출물
- `workspaces/DevB_김태혁/reports/W1_ai_bootup.md`
