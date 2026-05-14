# Lead 강태오 — W1 첫 작업 가이드

## 담당 (DECISIONS.md §0)
Gateway · BFF Aggregator · AI 서버 백업 · DevOps · PR 검증 스크립트.

## 첫 PR 권장 순서

### 1. Gateway 부팅 검증
- 위치: `services/gateway/`
- 채워 넣을 곳:
  - `auth/AuthController.java` — Google OAuth idToken 검증 + USERS upsert + RS256 토큰 발급
  - `auth/JwksController.java` — RSA KeyPair 로드 (K8s Secret), 실제 n/e 반환
  - `config/SecurityConfig.java` — Refresh blacklist 필터, CORS 화이트리스트
  - `filter/SoftLoginGatewayFilterFactory.java` — application.yml routes에 등록
- 검증:
  ```bash
  ./gradlew :gateway:bootRun
  curl http://localhost:8080/.well-known/jwks.json
  ```

### 2. BFF Aggregator 부팅 검증
- 위치: `services/bff-aggregator/`
- 채워 넣을 곳:
  - `usecase/TodayQtUseCase.java` — 오늘 QT 본문 ref 소스 결정 (bible_today_qt_schedule 조회)
  - `usecase/PassageUseCase.java` — 소프트 로그인 분기 검증
  - `websocket/StompAuthInterceptor.java` — CONNECT 헤더 JWT 검증
- 검증:
  ```bash
  curl http://localhost:8083/api/v1/qt/today
  ```

### 3. AI 서버 통합 지원
- 강상민이 DeepSeek 호출을 시작할 때 SSE timeout / Flux dispose hook을 페어로 검증.
- `services/ai-service/application/AiSessionService.java`의 emitter.onTimeout/onError 동작 확인.

### 4. 인프라
- `infra/docker-compose.yml`로 MySQL 8.0 / Redis 6379 / Kafka KRaft / Jaeger 띄우기.
- `.env.example`에서 `DEEPSEEK_API_KEY` 키 확인 (실제 키는 K8s Secret).

## 금지 (AGENTS.md §금지 패턴)
- 독립 auth-service / journal-service 신규 구현 금지
- @Transactional 블록 내 KafkaTemplate.send 직접 호출 금지
- 평문 application.yml에 API key 적기 금지

## 산출물
- `workspaces/Lead_강태오/reports/W1_gateway_bff_bootup.md`에 부팅 결과 + 막힌 점 기록.
