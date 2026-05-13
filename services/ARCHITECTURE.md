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
| `gateway` | Gateway Auth, JWT filter, route config, rate limit policy |
| `bff-aggregator` | today QT/passages use cases, service clients, websocket notification bridge |
| `bible-service` | book/verse/commentary/explanation model, today Journal aggregate, Kafka idempotent consumer, Redis cache |
| `ai-service` | today QT session/turn model, DeepSeek streaming, ChromaDB RAG, completion event publisher |
