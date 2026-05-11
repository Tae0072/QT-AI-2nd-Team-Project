# AGENTS.md — QT-AI AI 에이전트 협업 가이드

> Cursor / GitHub Copilot / 기타 AI 에이전트가 이 저장소에서 작업할 때 반드시 참조해야 하는 컨텍스트 파일.
> **Claude Code 사용자는 `CLAUDE.md`도 함께 참조.**

---

## 프로젝트 개요

- **앱명:** QT-AI (큐티 AI) — 성경 묵상 AI 코칭 앱
- **아키텍처:** Flutter + Spring Boot MSA (모든 백엔드 Java/Spring) + Kafka
- **문서 레포:** https://github.com/Tae0072/2nd-Team-Project
- **개발 레포:** https://github.com/Tae0072/QT-AI-2nd-Team-Project (이 레포)

---

## 필수 참조 문서 (코드 생성 전 반드시 로드)

| 우선순위 | 파일 | 목적 |
|----------|------|------|
| 🔴 필수 | `DECISIONS.md` (이 레포) | 포트·TTL·스택·저작권 단일 기준 |
| 🔴 필수 | [문서 레포] `apis/{service}/openapi.yaml` | 작업 서비스 API 계약 |
| 🔴 필수 | [문서 레포] `events/schema/{topic}-value.json` | Kafka envelope 스키마 |
| 🟡 중요 | [문서 레포] `02_ERD_문서.md` | 테이블 구조·외래키·인덱스 |
| 🟡 중요 | [문서 레포] `03_아키텍처_정의서.md` | 서비스 경계·통신 패턴 |
| 🟢 참고 | [문서 레포] `docs/adr/` | 기술 결정 근거 (ADR) |

**문서 레포 base URL:** `https://github.com/Tae0072/2nd-Team-Project/blob/main/`

---

## 기술 스택 확정 목록 (환각 방지)

| 영역 | 확정 스택 | 버전 | 주의 |
|------|----------|------|------|
| JDK | Java | 21 LTS | Java 17 코드 생성 금지 |
| Framework | Spring Boot | 3.3.x | 2.x API 사용 금지 |
| Build | Gradle Kotlin DSL | 8.x | `build.gradle` (Groovy) 생성 금지 |
| RDBMS | **MySQL** | 8.0 | **PostgreSQL 절대 금지** |
| ORM | JPA + Hibernate | 6.x | `@SQLRestriction` 사용 (`@Where` 금지) |
| Migration | Flyway | 10.x | |
| Messaging | Kafka | KRaft 모드 | **ZooKeeper 금지** |
| Schema Registry | Apicurio Registry | 2.5+ | |
| Tracing | **Jaeger** + OpenTelemetry | — | **Tempo 금지** |
| Logs | Loki + Promtail | — | |
| Metrics | Prometheus + Micrometer | — | |
| AI LLM SDK | **`com.anthropic:anthropic-java`** | 최신 | Anthropic 공식 Java SDK |
| Vector Store | ChromaDB | — | Spring RestClient로 REST 호출 |
| Mobile | Flutter | 3.24+ | Dart null-safety 필수 |

> **모든 백엔드 서비스는 Spring Boot 3.3 / Java 21로 통일.** (v1.0 Python FastAPI 결정 → W0에 전환)

---

## 서비스별 담당자

| 서비스 디렉토리 | Owner | 담당 범위 |
|----------------|-------|----------|
| `services/gateway/` | 강태오 | JWT 필터, 라우팅, Rate Limit |
| `services/bff-aggregator/` | 강태오 | UseCase 패턴, CompletableFuture 병렬 호출 |
| `services/auth-service/` | 이지윤 | JWT RS256, Google OAuth, Refresh Rotation |
| `services/bible-service/` | 김태혁 | 성경 다중 JOIN, Redis 캐시 |
| `services/ai-service/` | 강상민 | Anthropic Java SDK, ChromaDB RAG, SSE, 큐티 A~D 프롬프트 |
| `services/journal-service/` | 이승욱 | 이벤트 소싱, Kafka 컨슈머, PESSIMISTIC_WRITE |
| `apps/mobile/` | 김지민 | Riverpod, Dio, SSE 수신, Sliver Scroll |

---

## ai-service 스택 (Spring Boot 3.3 / Java 21)

```
services/ai-service/
  build.gradle.kts            # com.anthropic:anthropic-java 포함
  settings.gradle.kts
  src/main/
    java/com/qtai/ai/
      AiServiceApplication.java
      controller/AiSessionController.java     # POST /ai/sessions
                                              # POST /ai/sessions/{id}/turns  ← SSE 스트리밍 (SseEmitter)
      service/
        ClaudeStreamService.java              # Anthropic Java SDK 래퍼
        ChromaDbClient.java                   # ChromaDB REST 호출 (RestClient)
      kafka/AiSessionCompletedPublisher.java  # ai.session.completed 발행
      prompts/QtPromptTemplates.java          # 큐티 A~D 시스템 프롬프트
    resources/
      application.yml
```

BFF → AI 서비스 호출:
```java
RestClient.post().uri("http://ai-service.qtai.svc.cluster.local:8085/ai/sessions").retrieve()
```

---

## Kafka 이벤트 Envelope 표준

```json
{
  "eventId":         "evt_01HZX...",
  "eventType":       "ai.session.completed",
  "eventVersion":    1,
  "schemaSubject":   "ai.session.completed-value",
  "occurredAt":      "2026-05-26T14:30:00Z",
  "traceId":         "0af7651916cd43dd...",
  "producerService": "ai-service",
  "idempotencyKey":  "ai.session.completed:{sessionId}",
  "data":            { }
}
```

> ⚠️ **`payload` 키 사용 금지 — `data`만 허용**

### 토픽별 idempotencyKey 형식

| 토픽 | 형식 | Producer |
|------|------|----------|
| `user.deactivated` | `user.deactivated:{userId}` | auth-service |
| `user.activity.tracked` | `read.passage:{userId}:{book}:{ch}:{v}:{epochMinute}` | bff-aggregator |
| `ai.session.completed` | `ai.session.completed:{sessionId}` | ai-service |
| `journal.created` | `journal.created:{journalId}` | journal-service |
| `journal.updated` | `journal.update:{ULID}` | journal-service |
| `journal.deleted` | `journal.delete:{journalId}:{epochMs}` | journal-service |
| `journal.creation.failed` | `journal.creation.failed:{sessionId}` | journal-service |
| `notification.requested` | `{type}:{userId}:{occurredAt}` | 다수 |

---

## API 에러 응답 표준 (RFC 7807 ProblemDetail)

```
Content-Type: application/problem+json
{
  "type": "https://api.qtai.app/errors/{slug}",
  "title": "...",
  "status": 4xx 또는 5xx,
  "code": "DOMAIN_ERROR_CODE",
  "traceId": "...",
  "timestamp": "2026-05-26T14:30:00.123Z"
}
```

> `application/json` + `ErrorResponse{code, message, traceId}` 구버전 패턴 생성 금지

---

## SSE 이벤트 계약 (AI Service `/turns` 엔드포인트)

| 이벤트 | 의미 |
|--------|------|
| `turn_started` | 응답 시작 신호 |
| `token` | 스트리밍 토큰 청크 |
| `rag_sources` | RAG 참조 출처 |
| `turn_completed` | 응답 완료 |
| `[DONE]` | SSE 스트림 종료 |

---

## 토큰 정책

| 항목 | 값 |
|------|----|
| Access Token TTL | **1800s (30분)** |
| Refresh Token TTL | **14일** |
| Refresh Blacklist | Redis-WS `auth:refresh:revoked:{jti}` TTL=만료까지 |
| Access Blacklist | **없음** (30분 단명) |
| JWT 알고리즘 | RS256 |

---

## 금지 패턴 (환각 체크리스트)

```
❌ @Transactional 없는 DB 변경 메서드
❌ @Transactional 블록 내 KafkaTemplate.send() 직접 호출
   → 반드시 @TransactionalEventListener(AFTER_COMMIT) 사용
❌ application.yml / 코드에 평문 API Key · 비밀번호
   → K8s Secret + envFrom: secretKeyRef 필수
❌ 서비스 간 직접 DB JOIN / 직접 @Repository 공유
   → RestClient 또는 Kafka 이벤트로만
❌ JOURNAL_EVENTS 테이블 수정·삭제 코드
   → append-only (이벤트 소싱)
❌ Kafka 컨슈머에 idempotencyKey 검증 없음
   → DataIntegrityViolationException catch + skip 패턴 필수
❌ Spring Boot 2.x 전용 API (WebMvcConfigurerAdapter, @EnableSwagger2 등)
❌ PostgreSQL dialect / ZooKeeper 설정 / Tempo tracing
❌ LLM 공급자 교체 (Anthropic Claude 고정)
❌ Kafka envelope에 payload 키 (data 사용)
❌ AI SSE 경로에 /messages (올바른 경로: /turns)
❌ 성경 데이터에 개역개정 / ESV / NIV
```

---

## 성경 데이터 저작권

| 데이터 | 허용 여부 |
|--------|-----------|
| KJV (영어) | ✅ Public Domain |
| 개역한글 | ⚠️ 비상업·교육 목적 + 출처 표기 필수 |
| Matthew Henry 주석 (영문) | ✅ Public Domain |
| **개역개정** | ❌ **사용 금지 — 대한성서공회 저작권** |
| **ESV / NIV** | ❌ **사용 금지 — 라이선스 비용** |
| 한글 주석 / 신학 논문 | ❌ 더미 데이터로 대체 |

---

## 워크스페이스 폴더 격리 규칙

```
workspaces/
├── Lead_강태오/   ← 강태오 전용
├── DevA_이지윤/   ← 이지윤 전용
├── DevB_김태혁/   ← 김태혁 전용
├── DevC_강상민/   ← 강상민 전용
├── DevD_이승욱/   ← 이승욱 전용
└── DevE_김지민/   ← 김지민 전용
```

1. **타인 폴더 접근 금지** — AI 에이전트도 동일
2. `workspaces/` 내 파일은 빌드·런타임·CI에 영향 없음
3. 작업 시작 전 `workflows/` → 작업 → `reports/` 순서 필수
4. 공통 템플릿 `_template.md` 수정 금지

세부 내용: `workspaces/README.md` 참조
