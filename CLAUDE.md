# CLAUDE.md — QT-AI AI 에이전트 협업 가이드 (Claude Code)

> **Claude Code가 이 저장소에서 작업할 때 가장 먼저 읽어야 하는 파일.**
> 코드 생성·파일 수정 전 반드시 아래 순서대로 컨텍스트를 로드하라.

---

## 🚨 최우선 3대 규칙

1. **`DECISIONS.md` 항상 먼저 읽어라** — 포트·TTL·스택·저작권의 단일 기준
2. **문서 레포 명세를 기준으로 코드 작성** — https://github.com/Tae0072/2nd-Team-Project
3. **서비스 경계·담당자 폴더를 절대 침범하지 말 것**

---

## 필수 참조 파일 로드 순서

| 순서 | 파일 | 위치 | 목적 |
|------|------|------|------|
| 1 | `DECISIONS.md` | 이 레포 루트 | 포트·TTL·스택·저작권 단일 기준 |
| 2 | `AGENTS.md` | 이 레포 루트 | 금지 패턴·담당자·Envelope 표준 |
| 3 | `openapi.yaml` | [문서 레포] `apis/{service}/openapi.yaml` | 작업 서비스의 API 계약 |
| 4 | `ERD` | [문서 레포] `02_ERD_문서.md` | DB 테이블 구조 |
| 5 | `이벤트 스키마` | [문서 레포] `events/schema/{topic}-value.json` | Kafka envelope |

**문서 레포:** https://github.com/Tae0072/2nd-Team-Project

---

## 프로젝트 구조 & 서비스 담당자

```
services/
├── gateway/           # Owner: 강태오 — JWT 필터, 라우팅, Rate Limit (Spring Boot 3.3)
├── bff-aggregator/    # Owner: 강태오 — UseCase 패턴, CompletableFuture 병렬 호출 (Spring Boot 3.3)
├── auth-service/      # Owner: 이지윤 — JWT RS256, Google OAuth, Refresh Rotation (Spring Boot 3.3)
├── bible-service/     # Owner: 김태혁 — 성경 다중 JOIN, Redis 캐시 (Spring Boot 3.3)
├── ai-service/        # Owner: 강상민 — FastAPI (Python 전담), ChromaDB RAG, SSE, 큐티 프롬프트
└── journal-service/   # Owner: 이승욱 — 이벤트 소싱, Kafka 컨슈머, @Lock PESSIMISTIC_WRITE (Spring Boot 3.3)

apps/
└── mobile/            # Owner: 김지민 — Flutter 3.24, Riverpod, Dio, SSE 수신

infra/
├── k8s/               # Owner: 강태오 — Helm 차트, K8s 매니페스트
├── kafka/             # Owner: 강태오 — KRaft 설정, Schema Registry
└── monitoring/        # Owner: 강태오 — Prometheus, Loki, Jaeger

workspaces/
├── Lead_강태오/        # 강태오 전용 — 타인 접근 금지
├── DevA_이지윤/        # 이지윤 전용 — 타인 접근 금지
├── DevB_김태혁/        # 김태혁 전용 — 타인 접근 금지
├── DevC_강상민/        # 강상민 전용 — 타인 접근 금지
├── DevD_이승욱/        # 이승욱 전용 — 타인 접근 금지
└── DevE_김지민/        # 김지민 전용 — 타인 접근 금지
```

---

## 서비스별 기술 스택 (환각 방지)

| 서비스 | 언어 | 프레임워크 | DB | 포트(로컬) |
|--------|------|------------|-----|------------|
| gateway | Java 21 | Spring Boot 3.3 | — | 8080 |
| bff-aggregator | Java 21 | Spring Boot 3.3 | — | 8083 |
| auth-service | Java 21 | Spring Boot 3.3 | MySQL 8.0 | 8081 |
| bible-service | Java 21 | Spring Boot 3.3 | MySQL 8.0 | 8082 |
| ai-service | Python 3.11+ | FastAPI | ChromaDB | 8085 |
| journal-service | Java 21 | Spring Boot 3.3 | MySQL 8.0 | 8084 |
| mobile | Dart | Flutter 3.24 | — | — |

---

## Spring Boot 서비스 필수 준수 사항

### Gradle
- **`build.gradle.kts` (Kotlin DSL)만 사용** — Groovy `build.gradle` 생성 금지

### JPA / DB
- `MySQLDialect` 사용 — `PostgreSQLDialect` 절대 금지
- `@SQLRestriction` 사용 — 구 `@Where` 금지 (Hibernate 6.x)
- Flyway 마이그레이션 파일: `V{n}__{description}.sql`

### 트랜잭션
- DB 변경 메서드에 반드시 `@Transactional` 선언
- `@Transactional` 블록 내부에서 `KafkaTemplate.send()` 직접 호출 금지
- Kafka 발행은 반드시 `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용

### Kafka 컨슈머 (journal-service)
- 컨슈머에 반드시 `idempotencyKey` 검증 로직 포함
- `DataIntegrityViolationException` catch + skip 패턴 필수
- `JOURNAL_EVENTS` 테이블은 **append-only** — 수정·삭제 코드 생성 금지

### 보안
- 평문 API Key · 비밀번호를 `application.yml` 또는 코드에 하드코딩 금지
- K8s Secret + `envFrom: secretKeyRef` 방식만 허용

### 에러 응답
```
Content-Type: application/problem+json
{
  "type": "https://api.qtai.app/errors/{slug}",
  "title": "...",
  "status": 4xx,
  "code": "ERROR_CODE",
  "traceId": "...",
  "timestamp": "2026-05-26T14:30:00.123Z"
}
```
> `application/json` + `ErrorResponse{code, message}` 패턴 생성 금지

---

## ai-service 전용 규칙 (Python FastAPI)

> ⚠️ **ai-service 디렉토리에 Java / Kotlin / Spring Boot 코드 절대 금지**

```
services/ai-service/
  main.py                      # FastAPI app 진입점
  requirements.txt
  routers/
    session.py                 # POST /ai/sessions, POST /ai/sessions/{id}/turns (SSE)
  rag/
    chroma_client.py
    embedder.py
  prompts/
    templates.py               # 큐티 A~D 시스템 프롬프트
  kafka/
    event_publisher.py         # ai.session.completed 발행
```

- LLM: Anthropic Claude API (SSE 스트리밍)
- Vector Store: ChromaDB
- Kafka 발행: `kafka-python` 또는 `confluent-kafka`
- BFF에서 호출: `RestClient → http://ai-service.qtai.svc.cluster.local:8085`

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

> **`payload` 키 절대 금지 — 반드시 `data` 사용**
> 전체 스키마 → [문서 레포 `events/schema/`](https://github.com/Tae0072/2nd-Team-Project/tree/main/events/schema)

---

## SSE 스트림 이벤트 계약 (AI Service)

```
turn_started   → 응답 시작 신호
token          → 스트리밍 토큰 청크
rag_sources    → RAG 참조 출처
turn_completed → 응답 완료
[DONE]         → SSE 스트림 종료
```

---

## API 라우팅 (주요)

| Path | 내부 서비스 | 비고 |
|------|------------|------|
| `GET /api/v1/passages/{book}/{ch}/{v}` | BFF Aggregator | 입체 묵상 집계 |
| `POST /ai/sessions/{id}/turns` | AI Service | **`/messages` 아님** |
| `POST /auth/logout` | Auth Service | |
| `WS /ws/notifications` | BFF Aggregator | STOMP CONNECT 헤더 인증 |
| `GET /api/v1/journals` | Journal Service | 수동 생성(`POST`) 없음 |

---

## 금지 패턴 체크리스트

```
❌ jdbc:postgresql:// or PostgreSQLDialect
❌ zookeeper.connect 설정
❌ management.tracing.exporter=tempo (Jaeger만 사용)
❌ @EnableSwagger2 (SpringDoc OpenAPI 3.0 사용)
❌ WebMvcConfigurerAdapter (Spring Boot 2.x)
❌ build.gradle (Groovy) 생성
❌ application.yml에 평문 Secret
❌ 서비스 간 직접 DB JOIN
❌ ai-service에 Spring Boot / Java 코드
❌ JOURNAL_EVENTS 수정·삭제
❌ Kafka envelope에 payload 키 사용
❌ 성경 데이터에 개역개정 / ESV / NIV 텍스트
❌ /messages 경로 (AI SSE는 /turns)
```

---

## 토큰 정책

| 항목 | 값 |
|------|----|
| Access Token TTL | **30분 (1800s)** |
| Refresh Token TTL | **14일** |
| Refresh Blacklist | Redis-WS `auth:refresh:revoked:{jti}` TTL=만료까지 |
| JWT 알고리즘 | RS256 |

---

## 성경 저작권 (법적 리스크)

| 데이터 | 허용 여부 |
|--------|-----------|
| KJV (영어) | ✅ Public Domain |
| 개역한글 | ⚠️ 비상업·교육 목적 + 출처 표기 |
| Matthew Henry 주석 (영문) | ✅ Public Domain |
| **개역개정** | ❌ **절대 금지 (대한성서공회 저작권)** |
| **ESV / NIV** | ❌ **절대 금지 (라이선스 비용 필요)** |
| 한글 주석 / 신학 논문 | ❌ 더미 데이터로 대체 |

---

## 워크스페이스 규칙

- `workspaces/{본인명}/` 폴더에서만 읽기·쓰기
- **타인 폴더 접근 금지** — AI 에이전트도 동일하게 적용
- 작업 시작 전 `workflows/YYYY-MM-DD-{task}.md` 작성
- 작업 종료 후 `reports/YYYY-MM-DD-{task}.md` 작성
- 템플릿: `workspaces/_template.md` 참조
