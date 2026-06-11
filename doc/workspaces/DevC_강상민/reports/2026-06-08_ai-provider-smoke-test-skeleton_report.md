# Report - 2026-06-08 ai-provider-smoke-test-skeleton

## 개요

- 작업명: `ai-provider-smoke-test-skeleton`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-provider-smoke-test-skeleton.md`
- 작업 브랜치: `test/ai-provider-smoke-skeleton`
- PR 대상: `dev`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: provider endpoint 오픈 시 AI HTTP adapter 실제 연결을 확인할 수 있는 opt-in smoke test 골격 추가

## 브랜치 처리

- `dev`에서 시작해 원격 최신 상태를 기준으로 확인했다.
- 기존 로컬 브랜치 `test/ai-provider-smoke-skeleton`로 이동했다.
- 브랜치 이동 후 `dev`를 merge했고 fast-forward로 완료되었다.
- merge 결과 현재 브랜치는 `origin/dev`의 `test(ai): HTTP client contract fixture 정리 (#356)` 커밋을 포함한다.

## 변경 요약

- `AiProviderSmokeTest`를 추가했다.
- `QTAI_PROVIDER_SMOKE_ENABLED=true`가 없으면 테스트가 실행되지 않도록 `@EnabledIfEnvironmentVariable`을 적용했다.
- QT context, Today QT status, Bible single/batch/range, Admin/Auth active/verify/verify-any read smoke 호출 골격을 추가했다.
- service token, provider base-url, smoke 입력 ID/date/role은 환경 변수로만 받도록 했다.
- Study publish/hide, Audit log write smoke는 실제 데이터 변경 위험 때문에 이번 작업에서 제외했다.

## Smoke 실행 환경 변수

| 구분 | 환경 변수 |
| --- | --- |
| 실행 토글 | `QTAI_PROVIDER_SMOKE_ENABLED=true` |
| 공통 인증 | `QTAI_AI_CLIENT_SERVICE_TOKEN` |
| QT base-url | `QTAI_AI_CLIENT_QT_BASE_URL` |
| Bible base-url | `QTAI_AI_CLIENT_BIBLE_BASE_URL` |
| Admin/Auth base-url | `QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL` |
| QT 입력 | `QTAI_PROVIDER_SMOKE_QT_PASSAGE_ID`, `QTAI_PROVIDER_SMOKE_QT_DATE` |
| Bible 입력 | `QTAI_PROVIDER_SMOKE_BIBLE_VERSE_ID`, `QTAI_PROVIDER_SMOKE_BIBLE_BATCH_VERSE_IDS`, `QTAI_PROVIDER_SMOKE_BIBLE_BOOK`, `QTAI_PROVIDER_SMOKE_BIBLE_CHAPTER`, `QTAI_PROVIDER_SMOKE_BIBLE_START_VERSE`, `QTAI_PROVIDER_SMOKE_BIBLE_END_VERSE` |
| Admin/Auth 입력 | `QTAI_PROVIDER_SMOKE_ADMIN_MEMBER_ID`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLE`, `QTAI_PROVIDER_SMOKE_ADMIN_ROLES` |
| 선택 timeout | `QTAI_PROVIDER_SMOKE_TIMEOUT_MS` |

## 제외 범위

- provider Controller 구현 없음
- production code 변경 없음
- 기본 CI에서 외부 provider 호출 없음
- DB schema, migration, seed 변경 없음
- service-token 발급, JWKS, gateway, Docker, Kubernetes 변경 없음
- Study publish/hide, Audit log write smoke 실행 없음

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat compileTestJava` | PASS |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest` | PASS: env 미설정 상태에서 skip/pass |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiHttpClientRuntimeToggleTest` | PASS |
| `git diff --check` | PASS |
| workflow/report placeholder 검색 | PASS: 매치 없음 |
| 금지 번역본/출처 명칭 검색 | PASS: 매치 없음 |

## 후속 작업

- provider endpoint가 실제로 열리면 위 환경 변수를 주입해 live smoke를 실행한다.
- Study publish/hide, Audit log write smoke는 테스트 데이터와 명시적 opt-in 정책이 정해진 뒤 별도 PR에서 추가한다.
