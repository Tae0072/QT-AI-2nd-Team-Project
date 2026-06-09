# Workflow - 2026-06-09 ai-service-cutover-readiness

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-cutover-readiness` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `docs/ai-service-cutover-readiness` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service`가 독립 실행 가능한 skeleton 단계까지 진행되어 gateway/provider 연결자가 전환 조건을 확인할 문서가 필요하다. |
| 기준 문서 | `2026-06-09_ai-service-runtime-smoke-readiness.md`, `qtai-server/apis/ai-service/openapi.yaml` |
| 대상 경로 | `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service`로 트래픽을 넘기기 전에 팀이 확인해야 하는 전환 조건, 환경 변수, smoke 순서, DB 확인, provider 의존성, gateway route 확인, rollback 기준을 checklist로 고정한다.

이번 작업은 문서 전용이다. 실제 gateway route 활성화, provider live endpoint 호출, 운영 DB 적용, service-token/JWKS 구현, monolith AI 코드 삭제는 하지 않는다.

## 범위

- `/api/v1/system/ai/**`, `/api/v1/system/validation-reference-jobs/**`, `/api/v1/admin/ai/**` cutover 준비 조건을 정리한다.
- ai-service runtime smoke readiness 결과를 전환 전 확인 절차에 연결한다.
- AI 소유 DB 7개 테이블의 전환 전/후 확인 항목을 정리한다.
- provider `/api/v1/system/**` endpoint 준비 여부를 전환 차단 조건으로 분리한다.
- gateway route 전환 전 확인 항목과 rollback 기준을 문서화한다.

## 제외 범위

- gateway route 실제 전환
- provider `/api/v1/system/**` live 호출
- 운영 DB migration 적용
- 운영 service-token/JWKS 구현
- Docker/K8s 배포 설정
- monolith AI controller/usecase/entity 삭제
- 코드, 테스트, OpenAPI 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-09_ai-service-cutover-readiness.md` | 작업 범위와 실행 기준 |
| Create | `doc/workspaces/DevC_강상민/2026-06-09_ai-service-cutover-readiness-checklist.md` | 팀 공유용 cutover readiness checklist |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-cutover-readiness_report.md` | 수행 결과와 검증 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `docs/ai-service-cutover-readiness` 브랜치를 생성한다.
2. workflow 문서를 작성한다.
3. cutover readiness checklist 문서를 작성한다.
4. report 문서를 작성한다.
5. 문서 placeholder, trailing whitespace, runtime smoke 산출물 존재 여부를 검증한다.
6. 지정 문서 3개만 stage한다.
7. `docs(ai): ai-service cutover readiness checklist 작성` 커밋을 생성한다.

## 수용 기준

- [ ] 3개 문서가 생성된다.
- [ ] checklist가 target endpoint, 전환 전 조건, env var, smoke 순서, DB 확인, provider readiness, gateway 확인, rollback 기준을 포함한다.
- [ ] 실제 라우팅, provider 호출, 운영 DB 적용, 코드 변경이 없다.
- [ ] placeholder 문구와 trailing whitespace가 없다.
- [ ] runtime smoke test/script 경로가 존재한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 문서 3개에 집중되어 있다.
- checklist, workflow, report가 같은 cutover 기준을 공유한다.
- 병렬 작성보다 직접 작성이 용어와 범위 일관성을 유지하기 쉽다.

### 직접 실행 판단

메인 에이전트가 workflow 작성, checklist 작성, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
git status --short --branch
git diff --check
Test-Path "qtai-server\ai-service\scripts\runtime-smoke-readiness.ps1"
Test-Path "qtai-server\ai-service\src\test\java\com\qtai\ai\AiServiceRuntimeSmokeReadinessTest.java"
$placeholderPattern = ("TB" + "D") + "|" + ("TO" + "DO") + "|" + ("추후 " + "정리") + "|" + ("나중에 " + "정리") + "|" + ("미" + "정")
rg -n $placeholderPattern `
  "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-cutover-readiness.md" `
  "doc\workspaces\DevC_강상민\2026-06-09_ai-service-cutover-readiness-checklist.md" `
  "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-cutover-readiness_report.md"
```

## 다음 작업으로 넘길 항목

- `gateway-ai-route-transition-skeleton`
- provider endpoint open 후 live smoke 연결
- 운영 DB 이관 절차 확정
