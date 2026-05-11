# AI Service — 강상민

## 역할
AI 코칭 서비스 (**Spring Boot 3.3 / Java 21**)

## 담당
- ChromaDB RAG (성경 구절 검색)
- Anthropic Claude API 호출 (SSE 스트리밍)
- 큐티 A~D 프롬프트 관리
- 세션 완료 시 `ai.session.completed` Kafka 이벤트 발행

## 포트
8085 (로컬 / K8s ClusterIP 8080)

## 참조 명세
- OpenAPI: https://github.com/Tae0072/2nd-Team-Project/blob/main/apis/ai/openapi.yaml
- DECISIONS.md §6 — AI Service 스택

## 핵심 라이브러리
- `com.anthropic:anthropic-java:2.30.0` — Claude API (SSE 스트리밍 지원)
- Spring `RestClient` — ChromaDB HTTP 호출
- Spring `SseEmitter` — 클라이언트로 SSE 스트리밍
- Spring Kafka — 이벤트 발행

## 디렉토리 구조
```
services/ai-service/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/
    ├── java/com/qtai/ai/
    │   ├── AiServiceApplication.java
    │   ├── controller/AiSessionController.java
    │   ├── service/
    │   │   ├── ClaudeStreamService.java   # Anthropic Java SDK 래퍼
    │   │   └── ChromaDbClient.java        # ChromaDB REST 호출
    │   ├── kafka/AiSessionCompletedPublisher.java
    │   └── prompts/QtPromptTemplates.java # 큐티 A~D
    └── resources/
        └── application.yml
```

## SSE 이벤트 계약
```
turn_started → token (반복) → rag_sources → turn_completed → [DONE]
```

## ⚠️ 주의
- Kafka envelope: `data` 키 사용 (payload 아님)
- Kafka 발행은 `@TransactionalEventListener(AFTER_COMMIT)` 패턴 사용
- 성경 데이터: 개역개정 / ESV / NIV 적재 금지 (DECISIONS.md §8)
