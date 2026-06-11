# Workflow - 2026-06-11 ai-admin-controller-cleanup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/ai-admin-controller-cleanup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 2026-06-10 코드리뷰 TODO 1: service-ai의 봉인된 Admin AI 컨트롤러 제거 |
| 기준 문서 | `doc/workspaces/DevC_강상민/2026-06-10_코드리뷰_TODO_강상민.md`, `doc/2026-06-10_서버_코드리뷰.md`, `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/service-ai/**`, `qtai-server/apis/ai-service/openapi.yaml`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`service-ai`에서 `/api/v1/admin/ai/**`로 매핑되어 있지만 `SecurityConfig`의 `denyAll` 정책으로 호출 불가능한 관리자 AI 컨트롤러를 제거한다. 관리자 AI 화면과 admin role 이중검증은 `admin-server`가 계속 담당하며, `service-ai`는 시스템/배치 AI API와 F-15 단발 Q&A 경로만 유지한다.

## 범위

- `service-ai`의 `AdminAiAssetController`, `AdminAiMonitoringController`, `AdminAiBatchRunLogController`, `AdminAiValidationChecklistController`를 삭제한다.
- 위 컨트롤러에서만 쓰는 `AdminAiAuthentication`, `AdminAiValidationChecklistRequest`, `RegenerateAiAssetRequest`, `RegenerateAiAssetResponse`, 관리자 인증 mock/client 잔재를 삭제한다.
- `qtai-server/apis/ai-service/openapi.yaml`에서 service-ai 관리자 asset review 경로와 `AI Admin` tag/schema를 제거한다.
- `SecurityConfig`의 `/api/v1/admin/** denyAll` 정책은 유지하고 주석을 현재 책임 분리에 맞게 정리한다.
- service-ai 웹 컨트롤러가 `/api/v1/admin/**`를 노출하지 않는 회귀 테스트를 추가한다.

## 제외 범위

- `admin-server`의 `/api/v1/admin/ai/**` 컨트롤러, OpenAPI, 테스트 변경.
- `service-ai`의 내부 AI 생성/검수 서비스, `domain.ai.api.admin/**` UseCase/DTO 삭제.
- TODO 2 AD-01/06 계약 문서 전달, TODO 3 배치 이중 실행 검증, TODO 4 F-15 Q&A 구현.
- `admin-web/**` 변경.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/web/AdminAi*.java` | 봉인된 관리자 AI HTTP 컨트롤러와 전용 인증/요청 DTO 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/web/RegenerateAiAsset*.java` | 관리자 asset regenerate HTTP 전용 DTO 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/client/admin/*.java` | 관리자 HTTP 진입점 제거 후 미사용 관리자 인증 client/mock 제거 |
| Modify | `qtai-server/service-ai/src/main/java/com/qtai/ai/SecurityConfig.java` | denyAll 정책 주석 정리 |
| Modify | `qtai-server/service-ai/src/test/java/com/qtai/ai/AiForbiddenFeatureTest.java` | service-ai admin 경로 노출 금지 검증 |
| Modify | `qtai-server/apis/ai-service/openapi.yaml` | service-ai 관리자 경로 계약 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-admin-controller-cleanup_report.md` | 구현/검증 결과 보고 |

## 구현 순서

1. workflow 문서를 저장한다.
2. service-ai 관리자 컨트롤러와 전용 보조 타입을 삭제한다.
3. service-ai OpenAPI에서 관리자 asset review 경로와 관련 schema를 제거한다.
4. `SecurityConfig` 주석을 현재 책임 분리에 맞게 정리한다.
5. `AiForbiddenFeatureTest`에 `/api/v1/admin/**` 컨트롤러 노출 금지 테스트를 추가한다.
6. 지정 검증 명령을 실행한다.
7. report를 작성하고 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AiForbiddenFeatureTest` | service-ai 웹 컨트롤러가 `/api/v1/admin/**` 경로를 노출하지 않음 |
| `SecurityFilterChainTest` | `/api/v1/admin/**` denyAll 정책이 ROLE_ADMIN/USER 모두 403으로 유지됨 |

## 수용 기준

- [ ] service-ai에 `/api/v1/admin/ai/**` 컨트롤러가 남지 않는다.
- [ ] service-ai 전용 OpenAPI에 `AI Admin` tag, 관리자 asset review path/schema가 남지 않는다.
- [ ] `SecurityConfig`의 `/api/v1/admin/** denyAll` 정책이 유지된다.
- [ ] `admin-server`와 `admin-web`은 변경하지 않는다.
- [ ] 지정 테스트와 빌드가 통과하거나, 실행 불가 사유가 report에 기록된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 삭제 대상이 service-ai web 계층과 service-ai OpenAPI에 집중되어 있어 순차 확인이 안전하다.
- 테스트와 OpenAPI 정리가 같은 책임 분리 의도를 공유해 한 작업자가 확인하는 편이 충돌 위험이 낮다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :service-ai:test --tests "*AiForbiddenFeatureTest" --tests "*SecurityFilterChainTest"
.\gradlew.bat :service-ai:build
cd ..
npx @stoplight/spectral-cli lint qtai-server/apis/ai-service/openapi.yaml --ruleset .spectral.yaml
git diff --check
git status --short
```

`.spectral.yaml`이 없으면 Spectral lint는 실행하지 않고, OpenAPI YAML parse로 대체 검증한다.

## 후속 작업으로 남길 항목

- AD-01/06 응답 계약 전달은 TODO 2 별도 문서 작업으로 처리한다.
- F-15 Q&A 구현은 요구사항 확정 후 별도 workflow로 진행한다.
