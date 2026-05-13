# AI Service — DeepSeek/RAG/SSE

Owner: 강태오·김태혁·강상민

## Scope

- DeepSeek API(OpenAI 호환) 호출과 SSE 토큰 스트리밍
- ChromaDB 기반 RAG 조회
- AI session/turn 저장
- 오늘 QT 본문에서만 AI 세션 생성 (`qtDate` + passage 검증)
- `ai.session.completed` Kafka 이벤트 발행 (`qtDate`, summary 포함)

## Stack

- Spring Boot 3.3 / Java 21
- Spring `RestClient` 또는 streaming 처리용 `WebClient`
- Spring `SseEmitter`
- ChromaDB REST API
- Spring Kafka
- MySQL 8.0

## Canonical Endpoints

- `POST /ai/sessions`
- `POST /ai/sessions/{sessionId}/turns`
- `POST /ai/sessions/{sessionId}/complete`
- `GET /ai/sessions/{sessionId}`
- `GET /ai/sessions`

## Guardrails

- Anthropic SDK 사용 금지
- `ANTHROPIC_API_KEY` 사용 금지
- 오늘 QT가 아닌 본문에서 세션 생성 금지 (`AI_PASSAGE_NOT_TODAY_QT`)
- 완료 이벤트로 새 Journal 생성을 기대하지 않기. Bible Service가 오늘 Journal에 요약을 첨부한다.
- SSE 경로 `/messages` 생성 금지
- Kafka envelope는 `data` 키 사용
