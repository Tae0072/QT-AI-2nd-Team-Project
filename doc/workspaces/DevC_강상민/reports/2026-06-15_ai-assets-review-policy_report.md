# 2026-06-15 AI 산출물 검증 승인/반려 정책 반영 보고

## 요약

관리자 웹 `AI 산출물 검증` 화면과 admin-server 목록/승인 API를 정리한 작업이다. 승인 가능 조건을 `VALIDATING + AUTO PASSED + ADVISOR PASSED`로 명확히 분리했고, 검증 결과별 승인/반려 버튼 노출 정책을 화면에 반영했다.

추가로 `검증중` 목록 조회 timeout 원인이던 최신 검증 로그 조인 병목을 줄였고, 승인/반려/숨김 목록은 최근 처리된 산출물이 먼저 보이도록 `reviewedAt` 기준 정렬로 조정했다. AI 운영 모니터링에는 산출물 상태 카운트를 추가했다.

## 반영 내용

| 영역 | 내용 |
| --- | --- |
| admin-server 목록 API | 목록 응답에 `autoValidationResult`, `advisorValidationResult` 추가 |
| admin-server 조회 성능 | checklistVersion 필터가 없을 때 asset page를 먼저 조회하고, 해당 page의 검증 로그만 후속 조회 |
| admin-server 정렬 | `APPROVED`, `REJECTED`, `HIDDEN` 목록은 `reviewedAt desc, id desc` 정렬 |
| 승인 정책 | 승인 시 `VALIDATING`, 최신 AUTO `PASSED`, 최신 ADVISOR `PASSED`를 모두 요구 |
| 관리자 웹 | `NEEDS_REVIEW`는 승인 disabled + tooltip, `ADVISOR REJECTED`는 승인 버튼 미노출, `VALIDATING`은 반려 가능 유지 |
| 운영 모니터링 | AI 산출물 상태 카운트(`VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN`) 추가 |
| OpenAPI | 산출물 목록 응답/모니터링 응답 스키마 갱신 |
| 로컬/데모 데이터 | `scripts/local-demo-ai-advisor-pass.sql` 추가. Flyway 운영 seed가 아닌 수동 local/demo 전용 스크립트 |

## 정책 결정

- 승인 가능: `asset.status = VALIDATING`, AUTO 최신 결과 `PASSED`, ADVISOR 최신 결과 `PASSED`
- `NEEDS_REVIEW`: 승인 불가, 반려 가능
- `ADVISOR REJECTED`: 승인 버튼 미노출, 반려 가능
- `/api/v1/system/ai/validation-logs` write API는 service-ai 소유로 유지한다.
- admin-server는 관리자 목록/조회/승인/반려/숨김 API에 집중한다.

## 주요 파일

| 파일 | 내용 |
| --- | --- |
| `admin-web/src/pages/AiAssetsPage.tsx` | 검증 결과 분리 표시, 승인 버튼 정책, tooltip 반영 |
| `admin-web/src/pages/adminPageContracts.ts` | 승인 가능/승인 버튼 노출 계약 함수 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | 목록 조회 성능 개선, AUTO/ADVISOR 결과 분리, reviewedAt 정렬 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiAssetReviewService.java` | 승인 검증 조건 강화 및 review reason optional 처리 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiMonitoringQueryRepository.java` | 산출물 상태 카운트 조회 추가 |
| `qtai-server/apis/api-v1/openapi.yaml` | API 응답 스키마 갱신 |
| `scripts/local-demo-ai-advisor-pass.sql` | local/demo 전용 ADVISOR PASSED 시연 데이터 스크립트 |

## 확인한 현상

- `30960`, `30936`은 승인 후 DB에서 `APPROVED`로 정상 전이됐다.
- 승인 목록에서 보이지 않던 이유는 기존 목록 정렬이 `createdAt desc, id desc`라 최근 승인 항목이 첫 페이지에 오지 않았기 때문이다.
- 정렬 변경 후 DB 기준 `APPROVED` 목록 상위는 `30936`, `30960`, `30970`, `31094` 순으로 확인했다.

## 검증

```powershell
npm.cmd test
```

결과: 통과.

```powershell
npm.cmd run typecheck
```

결과: 통과.

```powershell
npm.cmd run build
```

결과: 통과. sandbox에서는 esbuild `spawn EPERM`이 발생해 승인 실행으로 재시도했다. Vite chunk size warning은 기존 번들 크기 경고다.

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryRepositoryTest" --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryRepositoryFastPathTest" --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiMonitoringQueryRepositoryTest" --tests "com.qtai.domain.admin.internal.AdminDashboardServiceTest" --tests "com.qtai.domain.admin.web.AdminDashboardControllerTest"
```

결과: 통과.

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:bootJar
```

결과: 통과.

```powershell
git diff --check
git diff --cached --check
```

결과: 통과.

## 배포/재기동

로컬 Docker admin-server에는 아래 명령으로 새 코드를 반영했다.

```powershell
docker compose up -d --build service-admin
```

결과: `qtai-admin-server` healthy 확인.

## 참고

- `.env.example`의 로컬 CORS 변경은 이번 커밋에 포함하지 않았다.
- 로컬 Docker DB에 앞서 넣었던 `LOCAL_DEMO_ADVISOR_PASS_SEED` 로그는 20건으로 확인됐고, 3건만 남기는 삭제 명령은 승인되지 않아 실행하지 않았다.
