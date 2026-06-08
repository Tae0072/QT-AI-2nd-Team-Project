# Report - 2026-06-08 ai-service-extraction-skeleton

## 개요

- 작업명: `ai-service-extraction-skeleton`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-service-extraction-skeleton.md`
- 작업 브랜치: `feature/ai-service-extraction-skeleton`
- PR 대상: `dev`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: AI outbound client 계약을 자체 보유한 독립 `ai-service` Spring Boot 모듈 skeleton 추가

## 브랜치 처리

- `dev`에서 시작해 `origin/dev` 최신 상태를 확인했다.
- 새 브랜치 `feat/ai-service-extraction-skeleton`를 생성했다.
- CI Branch Name Convention 실패 대응으로 `feature/ai-service-extraction-skeleton`로 브랜치명을 변경했다.
- 기존 같은 이름의 브랜치가 없어 별도 merge 충돌은 없었다.

## 변경 요약

- `qtai-server/settings.gradle.kts`에 `:ai-service` 모듈을 추가했다.
- `qtai-server/ai-service` 모듈을 생성하고 Spring Boot application entrypoint, actuator health, 기본 application config를 추가했다.
- 기존 monolith의 `com.qtai.domain.ai.client` 패키지를 `ai-service`로 복사해 AI outbound client interface, mock, HTTP adapter를 새 모듈이 자체 보유하게 했다.
- `AiServiceClientConfiguration`으로 client 구성요소를 명시 import해 새 서비스가 암묵적 패키지 스캔에 의존하지 않게 했다.
- 기본 mock mode context load, client boundary, HTTP mode fail-fast 테스트를 추가했다.

## 제외 범위

- 기존 `qtai-server` AI internal, web, api 삭제 또는 이동 없음
- AI DB table migration 없음
- provider Controller 구현 없음
- 실제 provider endpoint 호출 없음
- gateway route 전환 없음
- service-token 발급, JWKS, Docker, Kubernetes 변경 없음
- DeepSeek 호출 또는 AI business flow 전환 없음

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | PASS |
| `.\gradlew.bat :ai-service:test` | PASS |
| `.\gradlew.bat :ai-service:test --tests com.qtai.domain.ai.client.http.AiHttpSupportTest` | PASS |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest` | PASS |
| `git diff --check` | PASS |
| ai-service 도메인 금지 import 검색 | PASS: 매치 없음 |
| 금지 번역본/출처/민감 키워드 검색 | PASS: 매치 없음 |

## REQUEST_CHANGES 대응

- 리뷰 지적: `AiHttpSupport` 단위 테스트 부재와 write 요청 멱등성 키 설계 결함.
- 대응: `AiHttpSupportTest`를 추가해 인증 header, `traceparent`, success envelope parsing, error envelope mapping, malformed envelope mapping, 멱등성 키 전송을 단위 수준에서 검증했다.
- 대응: `AiHttpSupport`의 write 요청 멱등성 키 fallback을 매 호출 UUID 생성 방식에서 operation + stable payload hash 기반 deterministic key 방식으로 변경했다.
- 대응: `StudyPublishClientHttpAdapter`는 `aiAssetId` 기반 `study.publish` / `study.hide` 멱등성 키를 명시적으로 전달한다.
- 대응: `AuditLogClientHttpAdapter`는 audit command의 주요 필드를 해시 입력으로 사용해 동일 audit command 재시도 시 동일 멱등성 키를 전달한다.
- 범위: 이번 PR에서 추가한 `ai-service` 모듈의 client copy만 변경했다. 기존 monolith `qtai-server/src/main/java/com/qtai/domain/ai/client/**`는 skeleton PR 범위 밖이므로 변경하지 않았다.

## 보정 사항

- 최초 context boundary 테스트에서 client component scan이 기대대로 동작하지 않아 mock bean이 등록되지 않았다.
- `AiServiceClientConfiguration`을 추가해 AI client 구성요소를 명시 import하는 방식으로 보정했다.
- 이 방식은 skeleton 단계에서 복사된 client 패키지 범위를 명확히 하고, 다른 도메인 component를 실수로 스캔하지 않는 장점이 있다.

## 후속 작업

- provider endpoint가 열리면 `ai-service` 기준 live smoke를 실행한다.
- gateway route 전환은 provider readiness와 service-token/JWKS 준비 후 별도 PR에서 진행한다.
- 중복된 AI client 코드는 전환 완료 후 `qtai-server` 잔여 제거 또는 공통 모듈 추출 여부를 결정한다.
