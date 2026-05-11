# QT-AI Service Mock Architecture

All backend services use Java 21 and Spring Boot 3.3.x. Each service should grow from the same basic layer model:

```text
src/main/java/com/qtai/{service}/
├── presentation/          # REST controllers and request/response DTOs
├── application/           # use cases and service orchestration
│   └── port/              # inbound/outbound interfaces
├── domain/                # business model, domain rules, repository interfaces
└── infrastructure/        # JPA, Redis, Kafka, RestClient, external SDK adapters
```

The packages added here are placeholders for W1 work. Keep controllers thin, put business flow in use cases, keep domain code free from Spring Web dependencies, and connect external systems through infrastructure adapters.

Service-specific focus:

| Service | Main W1 Fill-in |
| --- | --- |
| `gateway` | JWT filter, route config, rate limit policy |
| `bff-aggregator` | passage/dashboard use cases, service clients, websocket notification bridge |
| `auth-service` | user model, refresh token rotation, JWT/OAuth adapters |
| `bible-service` | book/verse/commentary model, JPA repository, Redis cache |
| `ai-service` | session/turn model, Claude streaming, ChromaDB RAG, completion event publisher |
| `journal-service` | journal aggregate, append-only events, Kafka idempotent consumer, outbox/scheduler |
