# QT-AI (큐티 AI) — 2nd Team Project

> 성경 묵상을 AI 코칭으로 깊게 — Flutter + Spring Boot MSA + DeepSeek API

## 프로젝트 개요

QT-AI는 사용자가 매일 성경 묵상(QT)을 AI 코치와 함께 수행하고, 묵상 노트를 자동으로 생성·관리하는 앱입니다.

| 항목 | 내용 |
| --- | --- |
| 개발 기간 | 2026.05.08 (W0) ~ 2026.06.17 (발표) |
| 발표일 | 2026.06.17 (수) |
| 플랫폼 | iOS / Android (Flutter) |

## 팀 구성

| 이름 | 역할 | 담당 서비스 | 개인 워크스페이스 |
| --- | --- | --- | --- |
| 강태오 | Lead / Gateway / BFF / DevOps | `services/gateway/`, `services/bff-aggregator/`, `infra/` | `workspaces/Lead_강태오/` |
| 이지윤 | Backend / Bible | `services/bible-service/` | `workspaces/DevA_이지윤/` |
| 김태혁 | Backend / AI | `services/ai-service/` | `workspaces/DevB_김태혁/` |
| 강상민 | Backend / AI/RAG | `services/ai-service/` | `workspaces/DevC_강상민/` |
| 이승욱 | Backend / Bible + Kafka | `services/bible-service/` | `workspaces/DevD_이승욱/` |
| 김지민 | Frontend | `apps/mobile/` | `workspaces/DevE_김지민/` |

> 2026-05-12 기준: 독립 Auth Service와 Journal Service는 만들지 않습니다. Auth는 Gateway Auth 모듈, Journal은 Bible Service 내부 도메인입니다.

## 관련 저장소

| 레포 | 용도 | URL |
| --- | --- | --- |
| 이 레포 | 실제 개발·구현 작업 | https://github.com/Tae0072/QT-AI-2nd-Team-Project |
| 문서·명세 참조 | OpenAPI, ERD, ADR, 기획서 등 | https://github.com/Tae0072/2nd-Team-Project |

코드를 작성하기 전에 `DECISIONS.md`, `AGENTS.md`, 문서 레포의 OpenAPI/Kafka schema를 먼저 확인하세요.

## 디렉토리 구조

```text
QT-AI-2nd-Team-Project/
├── AGENTS.md
├── CLAUDE.md
├── DECISIONS.md
├── .env.example
├── .github/
├── services/
│   ├── gateway/          # Gateway Auth + routing
│   ├── bff-aggregator/   # dashboard/passages aggregation + WS notification
│   ├── bible-service/    # Bible + commentary + journal + shares
│   └── ai-service/       # DeepSeek + RAG + SSE
├── apps/mobile/
├── infra/
└── workspaces/
```

## 핵심 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Spring Boot 3.3 / Java 21 / MySQL 8.0 |
| AI | DeepSeek API(OpenAI 호환), Spring RestClient/WebClient, SSE |
| Vector Store | ChromaDB |
| Messaging | Kafka KRaft / Apicurio Schema Registry |
| Infrastructure | Kubernetes (Minikube) / Helm |
| Monitoring | Prometheus + Loki + Jaeger |
| Mobile | Flutter 3.24 / Dart |

> PostgreSQL, ZooKeeper, Tempo, Anthropic SDK, 독립 auth-service/journal-service는 금지입니다.

## 빠른 시작

```bash
cp .env.example .env
```

```bash
cd infra
docker compose up -d
docker compose --profile observability up -d
```

서비스별 빌드/테스트:

```bash
./gradlew -p services/gateway test
./gradlew -p services/bff-aggregator test
./gradlew -p services/bible-service test
./gradlew -p services/ai-service test
```

AI 에이전트 사용 시 `AGENTS.md`와 `DECISIONS.md`를 먼저 읽고 작업하세요.
