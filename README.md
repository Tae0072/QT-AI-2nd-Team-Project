# QT-AI (큐티 AI) — 2nd Team Project

> 성경 묵상을 AI 코칭으로 깊게 — Flutter + Spring Boot MSA + Anthropic Claude API

## 프로젝트 개요

QT-AI는 사용자가 매일 성경 묵상(QT)을 AI 코치와 함께 수행하고, 묵상 노트를 자동으로 생성·관리하는 앱입니다.

| 항목 | 내용 |
|------|------|
| 개발 기간 | 2026.05.08 (W0) ~ 2026.06.17 (발표) |
| 발표일 | 2026.06.17 (수) |
| 플랫폼 | iOS / Android (Flutter) |

---

## 팀 구성

| 이름 | 역할 | 담당 서비스 | 개인 워크스페이스 |
|------|------|-------------|-------------------|
| 강태오 | Lead / Gateway / BFF / DevOps | `services/gateway/`, `services/bff-aggregator/`, `infra/` | `workspaces/Lead_강태오/` |
| 이지윤 | Backend | `services/auth-service/` | `workspaces/DevA_이지윤/` |
| 김태혁 | Backend | `services/bible-service/` | `workspaces/DevB_김태혁/` |
| 강상민 | AI / Python | `services/ai-service/` | `workspaces/DevC_강상민/` |
| 이승욱 | Backend / Kafka | `services/journal-service/` | `workspaces/DevD_이승욱/` |
| 김지민 | Frontend | `apps/mobile/` | `workspaces/DevE_김지민/` |

---

## 레포지토리 역할 분리

| 레포 | 용도 | URL |
|------|------|-----|
| **이 레포** | 실제 개발·구현 작업 | https://github.com/Tae0072/QT-AI-2nd-Team-Project |
| 문서·명세 참조 | OpenAPI, ERD, ADR, 기획서 등 모든 문서 | https://github.com/Tae0072/2nd-Team-Project |

> 코드를 작성하기 전에 반드시 **문서 레포의 명세**를 먼저 확인하세요.

---

## 주요 참조 문서 (문서 레포)

| 문서 | 내용 |
|------|------|
| [`DECISIONS.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/DECISIONS.md) | **단일 기준** — 포트·TTL·스택·저작권 모든 결정값 |
| [`01_프로젝트_기획서.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/01_%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8_%EA%B8%B0%ED%9A%8D%EC%84%9C.md) | 기획·일정·팀 구성 |
| [`02_ERD_문서.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/02_ERD_%EB%AC%B8%EC%84%9C.md) | DB 테이블 구조·외래키·인덱스 |
| [`03_아키텍처_정의서.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/03_%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98_%EC%A0%95%EC%9D%98%EC%84%9C.md) | MSA 설계·서비스 경계·통신 패턴 |
| [`04_API_명세서.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/04_API_%EB%AA%85%EC%84%B8%EC%84%9C.md) | REST API 명세 요약 |
| `apis/{service}/openapi.yaml` | 서비스별 OpenAPI 3.0 명세 (코드 생성 기준) |
| `events/schema/*.json` | Kafka 이벤트 스키마 |
| `docs/adr/` | Architecture Decision Records |
| [`11_개발_환경_셋업_가이드.md`](https://github.com/Tae0072/2nd-Team-Project/blob/main/11_%EA%B0%9C%EB%B0%9C_%ED%99%98%EA%B2%BD_%EC%85%8B%EC%97%85_%EA%B0%80%EC%9D%B4%EB%93%9C.md) | 로컬 환경 구성 가이드 |

---

## 디렉토리 구조

```
QT-AI-2nd-Team-Project/
├── CLAUDE.md                  # AI 에이전트 필독 (Claude Code)
├── AGENTS.md                  # AI 에이전트 필독 (Cursor / Copilot)
├── DECISIONS.md               # 포트·TTL·스택 단일 기준표
├── README.md                  # 이 파일
├── .env.example               # 환경변수 템플릿
├── .github/
│   ├── CODEOWNERS             # 서비스별 담당자
│   ├── pull_request_template.md
│   └── workflows/
│       └── ci.yml             # CI 파이프라인
├── services/
│   ├── gateway/               # 강태오 — API Gateway (Spring Boot)
│   ├── bff-aggregator/        # 강태오 — BFF Aggregator (Spring Boot)
│   ├── auth-service/          # 이지윤 — Auth Service (Spring Boot)
│   ├── bible-service/         # 김태혁 — Bible Service (Spring Boot)
│   ├── ai-service/            # 강상민 — AI/RAG Service (Python FastAPI)
│   └── journal-service/       # 이승욱 — Journal + Kafka (Spring Boot)
├── apps/
│   └── mobile/                # 김지민 — Flutter 앱
├── infra/
│   ├── k8s/                   # Kubernetes Helm 차트 + 매니페스트
│   ├── kafka/                 # Kafka KRaft 설정
│   └── monitoring/            # Prometheus + Loki + Jaeger
└── workspaces/                # 개인 작업 폴더 (타인 폴더 접근 금지)
    ├── README.md
    ├── _template.md
    ├── Lead_강태오/
    ├── DevA_이지윤/
    ├── DevB_김태혁/
    ├── DevC_강상민/
    ├── DevD_이승욱/
    └── DevE_김지민/
```

---

## 아키텍처 개요

```
[Flutter App]
      │
      ▼ HTTPS
[API Gateway :8080]  ─── JWT 검증 / Rate Limit
      │
      ├──▶ [BFF Aggregator :8083]   ─ CompletableFuture 병렬 집계
      ├──▶ [Auth Service    :8081]   ─ JWT RS256 / Google OAuth
      ├──▶ [Bible Service   :8082]   ─ 성경 조회 / Redis 캐시
      ├──▶ [AI Service      :8085]   ─ FastAPI / Claude API / SSE
      └──▶ [Journal Service :8084]   ─ 이벤트 소싱 / Kafka 컨슈머

[Kafka KRaft :9092]  ←─ ai.session.completed 등 8개 토픽
[Apicurio Schema Registry :8086]
[MySQL :3306] / [Redis-Cache :6379] / [Redis-WS :6380]
[ChromaDB :8000]
[Prometheus + Loki + Jaeger]
```

---

## 핵심 기술 스택

| 영역 | 기술 | 버전 |
|------|------|------|
| Backend (Java) | Spring Boot | 3.3.x |
| JDK | Java | 21 LTS |
| Build | Gradle Kotlin DSL | 8.x |
| Database | MySQL | 8.0 |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Migration | Flyway | 10.x |
| Backend (Python) | FastAPI | 최신 안정 |
| Vector Store | ChromaDB | — |
| LLM | Anthropic Claude API | — |
| Messaging | Kafka KRaft | — |
| Schema Registry | Apicurio Registry | 2.5+ |
| Container | Kubernetes (Minikube) + Helm | — |
| Monitoring | Prometheus + Loki + Jaeger | — |
| Mobile | Flutter | 3.24+ |

> ⚠️ **PostgreSQL 금지 · ZooKeeper 금지 · Tempo 금지 · Groovy Gradle 금지**

---

## 빠른 시작

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env 파일 열어 실제 값 입력 (API Key 등)
```

### 2. 상세 환경 구성

→ [11_개발_환경_셋업_가이드.md](https://github.com/Tae0072/2nd-Team-Project/blob/main/11_%EA%B0%9C%EB%B0%9C_%ED%99%98%EA%B2%BD_%EC%85%8B%EC%97%85_%EA%B0%80%EC%9D%B4%EB%93%9C.md) 참조

### 3. AI 에이전트 사용 시

`CLAUDE.md` 또는 `AGENTS.md`를 먼저 읽고 작업하세요.
