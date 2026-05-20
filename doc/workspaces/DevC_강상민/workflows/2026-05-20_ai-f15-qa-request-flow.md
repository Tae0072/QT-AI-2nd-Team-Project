# Workflow — 2026-05-20 ai-f15-qa-request-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-f15-qa-request-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | F-15, F-14 |
| 기준 문서 | `07_요구사항_정의서.md` §6.15, `04_API_명세서.md` §4.5, `05_시퀀스_다이어그램.md` §8 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

F-15 사실 기반 Q&A를 단발성 요청/조회 흐름으로 구현한다. 허용 질문은 단어, 시대상, 역사적 배경에 한정하고, 가치 판단·신앙 평가·상담·단정적 설교 요청은 표준 차단 사유로 기록한다.

## 문제 정의

F-15는 사용자에게 AI 기능을 제공하지만 자유 챗봇이 아니다. 이전 질문 맥락을 유지하지 않고, SSE를 사용하지 않으며, 응답 생성 직후 1층 형식 검증과 2층 자동 정책 검증을 통과한 답변만 표시해야 한다.

## API 범위

| API | 인증 | 책임 |
| --- | --- | --- |
| `POST /api/v1/ai/qa-requests` | USER | 질문 접수, 차단 또는 요청 생성 |
| `GET /api/v1/ai/qa-requests/{requestId}` | USER, 요청자 본인 | Q&A 상태 polling 조회 |

## 상태 기준

- `ai_qa_requests.status`: `REQUESTED`, `ANSWERED`, `BLOCKED`, `FAILED`
- 차단 시 `blockedReason`과 표준 안내 문구를 저장한다.
- LLM 응답이 검증을 통과하면 `ai_generated_assets.assetType=QA_RESPONSE`, `status=APPROVED`로 연결한다.
- 검증 실패 또는 외부 AI 실패는 `FAILED`로 기록하고 사용자에게 provider 원문을 노출하지 않는다.

## 질문 차단 기준

| 차단 유형 | 예 |
| --- | --- |
| `VALUE_JUDGMENT` | “제가 이 선택을 해도 되나요?” |
| `FAITH_EVALUATION` | “제 믿음이 부족한 건가요?” |
| `COUNSELING` | “제 인생 문제를 어떻게 해야 하나요?” |
| `SERMON_REQUEST` | “이 본문으로 설교문을 써 주세요.” |
| `MULTI_TURN_CONTEXT` | “아까 답변 기준으로 이어서 설명해 주세요.” |

## 허용 질문 기준

- 특정 단어의 의미 설명
- 시대상 또는 역사 배경 설명
- 본문 이해를 돕는 객관 정보
- 검증 가능한 출처 또는 근거를 붙일 수 있는 질문

## 처리 방식

1. 인증된 member id를 기준으로 요청을 생성한다.
2. 질문 정책 검사를 먼저 수행한다.
3. 차단 대상이면 LLM을 호출하지 않고 `BLOCKED`로 저장한다.
4. 허용 대상이면 `REQUESTED`와 `pollAfterSeconds=2`를 반환한다.
5. 5초 이내 완료 가능한 경우에만 `201 Created`와 `ANSWERED`를 반환할 수 있다.
6. 응답 생성 후 형식 검증과 정책 검증을 수행한다.
7. 통과한 답변만 `answer`, `sourceLabel`, `qaResponseAssetId`로 조회 가능하게 한다.

## 제외 범위

- 다중 턴 대화 세션 저장 금지
- SSE 또는 streaming 응답 금지
- `/ai/sessions/**` 엔드포인트 생성 금지
- 노트 작성 과정에 AI가 개입하는 기능 금지
- Q&A 원문을 모델 학습용으로 사용하는 정책 구현 금지

## 수용 기준

- [ ] 차단 질문은 LLM 호출 없이 `BLOCKED`로 저장된다.
- [ ] 허용 질문도 검증 전에는 답변이 사용자에게 노출되지 않는다.
- [ ] 이전 질문 맥락을 참조하는 필드나 세션 id가 없다.
- [ ] `GET` 결과는 요청자 본인만 조회할 수 있다.
- [ ] 실패 시 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 중 필요한 로그가 남는다.
- [ ] `AI_QUESTION_BLOCKED`, `EXTERNAL_AI_ERROR` 등 공통 ErrorCode 기준을 따른다.

## 테스트 계획

- Controller 테스트: 202 Accepted, 201 Created, 422 blocked, 본인 조회 제한
- Service 테스트: 허용 질문, 차단 질문, 검증 실패, 외부 AI 실패
- 경계 테스트: SSE, `/ai/sessions/**`, multi-turn context 필드 부재
- 보안 테스트: 다른 member의 requestId 조회 차단

## 검증 명령

- `./gradlew -p qtai-server test --tests "*AiQa*"`
- `./gradlew -p qtai-server build`
- `rg -n "SseEmitter|text/event-stream|/ai/sessions|sessionId|conversation" qtai-server/src/main/java && exit 1`

## PR 전 체크

- [ ] PR 설명에 F-15와 차단/검증 정책을 명시한다.
- [ ] 차단 안내 문구가 화면/API/로그에서 같은 의미로 유지된다.
- [ ] provider raw response와 검증 참조 원문이 사용자 응답에 없다.
