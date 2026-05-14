# AGENTS.md — QT-AI AI 에이전트 협업 가이드

> Cursor / GitHub Copilot / Claude Code / 기타 AI 에이전트가 이 저장소에서 작업할 때 반드시 참조해야 하는 컨텍스트 파일.
> 문서 레포 최신 기준과 충돌하면 `DECISIONS.md`가 우선이다.

---

## 프로젝트 개요

- **앱명:** QT-AI (큐티 AI) — 성경 묵상 AI 코칭 앱
- **아키텍처:** Flutter + Spring Boot MSA + Kafka
- **문서 레포:** https://github.com/Tae0072/2nd-Team-Project
- **개발 레포:** https://github.com/Tae0072/QT-AI-2nd-Team-Project
- **문서 레포 base URL:** `https://github.com/Tae0072/2nd-Team-Project/blob/master/`

---

## 필수 참조 문서

| 우선순위 | 파일 | 목적 |
| --- | --- | --- |
| 🔴 필수 | `DECISIONS.md` (이 레포) | 포트·TTL·스택·Route·Envelope 단일 기준 |
| 🔴 필수 | [문서 레포] `apis/{service}/openapi.yaml` | 작업 서비스 API 계약 |
| 🔴 필수 | [문서 레포] `events/schema/{topic}-value.json` | Kafka envelope 및 data 스키마 |
| 🟡 중요 | [문서 레포] `02_ERD_문서.md` | 테이블 구조·외래키 정책·인덱스 |
| 🟡 중요 | [문서 레포] `03_아키텍처_정의서.md` | 서비스 경계·통신 패턴 |
| 🟢 참고 | [문서 레포] `docs/adr/` | 기술 결정 근거 |
| 🟢 참고 | [문서 레포] `24_회의결정_2026-05-13_오전.md` | 2026-05-13 오전 회의 MVP 제품 범위 |

> **묵상 기록 API 기준:** 별도 Journal Service/OpenAPI를 사용하지 않는다. `/api/v1/journals...` 계약은 문서 레포 `apis/bible/openapi.yaml` 기준으로 Bible Service에서 구현한다. 자유 본문 `POST /api/v1/journals`는 만들지 않고, 오늘 QT DRAFT는 `POST /api/v1/journals/today`만 사용한다.

---

## 기술 스택 확정 목록

| 영역 | 확정 스택 | 버전 | 주의 |
| --- | --- | --- | --- |
| JDK | Java | 21 LTS | Java 17 코드 생성 금지 |
| Framework | Spring Boot | 3.3.x | Spring Boot 2.x API 사용 금지 |
| Build | Gradle Kotlin DSL | 8.x | `build.gradle` Groovy 생성 금지 |
| RDBMS | MySQL | 8.0 | PostgreSQL dialect/DDL 금지 |
| ORM | JPA + Hibernate | 6.x | `@SQLRestriction` 사용, `@Where` 금지 |
| Migration | Flyway | 10.x | |
| Messaging | Kafka | KRaft mode | ZooKeeper 금지 |
| Schema Registry | Apicurio Registry | 2.5+ | |
| Tracing | Jaeger + OpenTelemetry | — | Tempo 금지 |
| Logs | Loki + Promtail | — | |
| Metrics | Prometheus + Micrometer | — | |
| AI LLM | DeepSeek API | OpenAI 호환 | Anthropic SDK 코드 생성 금지 |
| LLM Client | Spring `RestClient` 또는 streaming 처리용 `WebClient` | — | 별도 Anthropic SDK 없음 |
| Vector Store | ChromaDB | — | Spring `RestClient`로 REST 호출 |
| Mobile | Flutter | 3.24+ | Dart null-safety 필수 |

> 모든 백엔드 서비스는 Spring Boot 3.3 / Java 21로 통일한다.

---

## 서비스별 담당자

| 서비스 디렉토리 | Owner | 담당 범위 |
| --- | --- | --- |
| `services/gateway/` | 강태오 | Gateway Auth(JWT·Google OAuth·Refresh), 라우팅, Rate Limit |
| `services/bff-aggregator/` | 강태오 | UseCase 패턴, CompletableFuture 병렬 호출, WebSocket 알림 |
| `services/bible-service/` | 이지윤·이승욱 | 성경 본문, 주석, Redis 캐시, 묵상일지(Journal) 통합, Kafka 컨슈머 |
| `services/ai-service/` | 강태오(팀장)·김태혁·강상민 | DeepSeek API, ChromaDB RAG, SSE, 오늘 QT AI 질문 |
| `apps/mobile/` | 김지민 | Riverpod, Dio, SSE 수신, Sliver Scroll, 관리자 웹 보조 |

> **Auth Service 제거 (2026-05-12):** 독립 `services/auth-service/` 신규 구현 금지. 인증은 Gateway Auth 모듈에서 처리한다.
> **Journal Service 제거 (2026-05-12):** 독립 `services/journal-service/` 신규 구현 금지. 묵상일지는 Bible Service 도메인이다.

---

## 2026-05-13 MVP 제품 범위

- 앱 첫 화면은 별도 홈이 아니라 오늘 QT 화면이다. 오늘 QT 본문/설명을 먼저 로딩하고 나머지는 백그라운드로 로딩한다.
- 오늘 QT는 **하루 1개 본문(범위 허용)** 이다 — 한 절·한 단락·다중 장 모두 가능 (DECISIONS.md §3.1, 02_ERD v2.3, ADR-0021). API/DB는 `chapterStart`·`verseStart`·`chapterEnd`·`verseEnd` + `startOrdinal`/`endOrdinal`을 사용한다. 한 절이면 start == end로 들어온다. 유일한 좌표 제약은 `(chapterStart, verseStart) ≤ (chapterEnd, verseEnd)`. **"오늘 QT 1개"** 원칙은 유지(qt_date PK).
- 일반 성경 보기/검색은 읽기 전용이다. AI 질문과 Journal 생성은 오늘 QT 본문에서만 허용한다.
- AI 세션 생성은 `qtDate`와 passage가 오늘 QT와 일치해야 한다. 불일치 시 `AI_PASSAGE_NOT_TODAY_QT`.
- 본문 설명(요약, 배경, 어려운 단어, 출처)은 Bible DB 저장 데이터다. AI는 적용 질문과 묵상 보조 응답에 집중한다.
- Journal은 오늘 QT 기준 4필드(`felt`, `memorableVerse`, `application`, `prayer`) 자동 저장이다. 사용자에게 글자 수 제한을 노출하지 않는다.
- `ai.session.completed`는 새 Journal 생성이 아니라 오늘 Journal에 `aiSessionId`와 AI 요약을 첨부한다.
- 찬양은 AI 추천곡 저장/제거만 MVP에 포함한다. 직접 YouTube URL 입력, 가사/음원/스트리밍 제공은 제외한다.
- 교회 인증은 MVP 기본 제외다. 버튼 자리는 둘 수 있지만 인증 여부로 앱 사용을 막지 않는다.

---

## AI Service 스택

```text
services/ai-service/
  build.gradle.kts                    # DeepSeek API 호출, Anthropic SDK 없음
  settings.gradle.kts
  src/main/
    java/com/qtai/ai/
      AiServiceApplication.java
      presentation/AiSessionController.java     # POST /ai/sessions, POST /ai/sessions/{id}/turns
      application/usecase/
      domain/model/
      infrastructure/
        llm/DeepSeekStreamService.java          # DeepSeek OpenAI 호환 API 래퍼
        rag/ChromaDbClient.java                 # ChromaDB REST 호출
        kafka/AiSessionCompletedPublisher.java
      prompt/QtPromptTemplates.java
    resources/application.yml
```

BFF → AI 서비스 호출 예:

```java
RestClient.post()
    .uri("http://ai-service.qtai.svc.cluster.local:8085/ai/sessions")
    .retrieve();
```

---

## Kafka 이벤트 Envelope 표준

```json
{
  "eventId": "evt_01HZX...",
  "eventType": "ai.session.completed",
  "eventVersion": 1,
  "schemaSubject": "ai.session.completed-value",
  "occurredAt": "2026-05-26T14:30:00Z",
  "traceId": "0af7651916cd43dd...",
  "producerService": "ai-service",
  "idempotencyKey": "ai.session.completed:{sessionId}",
  "data": {}
}
```

> Envelope에서 `payload` 키 사용 금지. 표준 키는 `data`다.

| 토픽 | idempotencyKey 형식 | Producer |
| --- | --- | --- |
| `user.activity.tracked` | `read.passage:{userId}:{book}:{ch}:{v}:{epochMinute}` | bff-aggregator |
| `ai.session.completed` | `ai.session.completed:{sessionId}` | ai-service |
| `journal.created` | `journal.created:{journalId}` | bible-service |
| `journal.updated` | `journal.update:{ULID}` | bible-service |
| `journal.deleted` | `journal.delete:{journalId}:{epochMs}` | bible-service |
| `journal.creation.failed` | `journal.creation.failed:{sessionId}` | bible-service |
| `notification.requested` | `{type}:{userId}:{occurredAt}` | 다수 |

> `user.deactivated`는 MVP 제외. 계정 탈퇴와 해당 이벤트는 v1.1 이후 검토한다.

---

## API 에러 응답 표준

```http
Content-Type: application/problem+json
```

```json
{
  "type": "https://api.qtai.app/errors/{slug}",
  "title": "...",
  "status": 400,
  "code": "DOMAIN_ERROR_CODE",
  "traceId": "...",
  "timestamp": "2026-05-26T14:30:00.123Z"
}
```

> `application/json` + `ErrorResponse{code, message, traceId}` 구버전 패턴 생성 금지.

---

## SSE 이벤트 계약

AI Service endpoint는 `POST /ai/sessions/{id}/turns`다. `/messages` 경로를 만들지 않는다.

| 이벤트 | 의미 |
| --- | --- |
| `turn_started` | 응답 시작 |
| `token` | 스트리밍 토큰 청크 |
| `rag_sources` | RAG 참조 출처 |
| `turn_completed` | 응답 완료와 DB 적재 완료 |
| `error` | 스트림 중 오류 |
| `end` | `data: [DONE]` 종료 신호 |

---

## 금지 패턴

```text
❌ @Transactional 없는 DB 변경 메서드
❌ @Transactional 블록 내 KafkaTemplate.send() 직접 호출
   → 반드시 @TransactionalEventListener(AFTER_COMMIT) 사용
❌ application.yml / 코드에 평문 API Key · 비밀번호
   → K8s Secret + envFrom: secretKeyRef 사용
❌ 서비스 간 직접 DB JOIN / 직접 @Repository 공유
   → RestClient 또는 Kafka 이벤트로만
❌ JOURNAL_EVENTS 테이블 수정·삭제 코드
   → append-only 이벤트 소싱
❌ Kafka 컨슈머 idempotencyKey 검증 누락
   → DataIntegrityViolationException catch + skip 패턴
❌ 독립 auth-service / journal-service 신규 구현
❌ 자유 본문 `POST /api/v1/journals` 생성
❌ 오늘 QT가 아닌 본문에서 AI 세션 또는 Journal 생성
❌ Journal 화면에 사용자 노출 글자 수 제한/저장 버튼 강제
❌ 직접 YouTube URL 입력, 가사/음원/스트리밍 제공
❌ 교회 인증을 필수 가입/사용 gate로 처리
❌ 별도 홈 화면을 첫 화면으로 강제
❌ Spring Boot 2.x 전용 API
❌ PostgreSQL dialect / ZooKeeper 설정 / Tempo tracing
❌ Anthropic SDK 또는 Claude 고정 코드
❌ Kafka envelope에 payload 키
❌ AI SSE 경로에 /messages
❌ 성경 데이터에 개역개정 / ESV / NIV
```

---

## 성경 데이터 저작권

| 데이터 | 허용 여부 |
| --- | --- |
| KJV (영어) | ✅ Public Domain |
| 개역한글 | ⚠️ 비상업·교육 목적 + 출처 표기 필수 |
| Matthew Henry 주석 (영문) | ✅ Public Domain |
| 개역개정 | ❌ 사용 금지 |
| ESV / NIV | ❌ 사용 금지 |
| 한글 주석 / 신학 논문 | ❌ 더미 데이터로 대체 |

---

## 워크스페이스 폴더 격리 규칙

```text
workspaces/
├── Lead_강태오/
├── DevA_이지윤/
├── DevB_김태혁/
├── DevC_강상민/
├── DevD_이승욱/
└── DevE_김지민/
```

1. 타인 폴더 접근 금지. AI 에이전트도 동일하다.
2. `workspaces/` 내 파일은 빌드·런타임·CI에 영향이 없어야 한다.
3. 작업 시작 전 `workflows/` 작성 → 작업 → `reports/` 작성 순서를 지킨다.
4. 공통 템플릿 `_template.md`는 수정하지 않는다.

세부 내용은 `workspaces/README.md`를 참조한다.
