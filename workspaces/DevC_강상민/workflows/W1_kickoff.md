# DevC 강상민 — W1 첫 작업 가이드 (AI 주도)

## 담당 (DECISIONS.md §0 — 2026-05-14 재배치)
AI 해설 생성, 편집자 에이전트, AI 자동 검증 메커니즘 정의. AI 서버 메인.

## 첫 PR 권장 순서

### 1. 프롬프트 템플릿 적재
- 위치: `services/ai-service/prompt/QtPromptTemplates.java`
- 현재 fallback 4단계(A/B/C/D) 들어가 있음. Flyway `V1__init_schema.sql`의 `ai_prompt_templates` 테이블에 row INSERT.
- 09_AI_프롬프트_운영_가이드 §3 기준 — 본문 한정·범위 이탈 거절·차분한 정보 전달형 톤 강제.

### 2. 해설 생성 파이프라인
- Bible Service의 `bible_explanations`에서 `source_type=REFERENCE_SOURCE` row를 컨텍스트로 적재
  (Tyndale / Matthew Henry / Bible Dictionary, 범위 포함되는 row 전부).
- DeepSeek로 한국어 해설 생성 → 본인이 만든 row는 `source_type=GENERATED_EXPLANATION, editor_verified_at=NULL`로 저장.
- 편집자 에이전트(이지윤 큐레이션 골든셋 + 자동 검증)가 OK 표시 시 `editor_verified_at`을 채워야 사용자 노출.

### 3. SSE 이벤트 4종 합의
- AGENTS.md / 04 § 6.3 기준: `turn_started`, `token`, `sources`(구 rag_sources), `turn_completed`, `error`, `end`.
- 김지민과 페어 — Flutter SSEClient 측 이벤트 파싱과 정합.

### 4. ai.session.completed Kafka 발행
- `infrastructure/kafka/AiSessionCompletedPublisher.java` 채우기.
- envelope: data 키 (payload 키 금지). idempotencyKey: `ai.session.completed:{sessionId}`.
- `@TransactionalEventListener(AFTER_COMMIT)` 패턴 강태오와 함께 검증.

## 금지
- Anthropic SDK / Claude 고정 코드 사용 — DeepSeek (OpenAI 호환) 단독
- AI 응답 시 REFERENCE_SOURCE 원문을 그대로 노출 — 컨텍스트 적재 전용 (저작권/PD 혼재)
- 오늘 QT 본문이 아닌 구절로 세션 시작 — 422 AI_PASSAGE_NOT_TODAY_QT 강제

## 산출물
- `workspaces/DevC_강상민/reports/W1_ai_main_bootup.md` — DeepSeek 토큰 스트림 첫 성공 로그.
