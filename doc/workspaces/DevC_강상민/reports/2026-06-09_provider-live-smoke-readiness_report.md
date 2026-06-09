# Report - 2026-06-09 provider-live-smoke-readiness

## 작업 요약

provider `/api/v1/system/**` endpoint가 열렸을 때 AI HTTP adapter live smoke를 실행할 수 있도록 readiness 문서와 opt-in wrapper를 추가했다.

이번 변경은 live smoke 준비 전용이다. provider endpoint 실제 호출, gateway route enable, 운영 DB migration, Kafka/event dependency 추가, monolith AI 삭제는 수행하지 않았다.

## 변경 내용

- `qtai-server/scripts/provider-live-smoke-readiness.ps1`을 추가했다.
- workflow 문서 `2026-06-09_provider-live-smoke-readiness.md`를 추가했다.
- readiness 문서 `2026-06-09_provider-live-smoke-readiness.md`를 추가했다.
- report 문서 `2026-06-09_provider-live-smoke-readiness_report.md`를 추가했다.

## 실행 guard

- `QTAI_PROVIDER_SMOKE_ENABLED=true`가 없으면 provider live smoke를 실행하지 않는다.
- `-AllowSkip` 옵션에서는 provider 호출 없이 guard 확인만 통과한다.
- enabled 상태에서는 필수 환경 변수 이름만 검증하고 값은 출력하지 않는다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat compileTestJava` | 통과 |
| `.\gradlew.bat :test --tests com.qtai.domain.ai.client.http.AiProviderSmokeTest` | 통과, env 미설정으로 skip/pass |
| `powershell -ExecutionPolicy Bypass -File .\scripts\provider-live-smoke-readiness.ps1 -AllowSkip` | 통과, provider 호출 없음 |
| `git diff --check` | 통과 |
| placeholder 문구 검색 | 통과 |

## 제외 확인

- provider endpoint를 호출하지 않았다.
- Study publish/hide, Audit write smoke를 실행하지 않았다.
- gateway route를 활성화하지 않았다.
- 운영 DB migration을 적용하지 않았다.
- Kafka, topic, event dependency를 추가하지 않았다.
- monolith AI 코드를 삭제하지 않았다.

## 다음 작업

- `ai-service-operational-handoff`
- provider endpoint open 후 `ai-provider-live-smoke`
- write smoke 정책과 테스트 데이터 승인
