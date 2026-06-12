# Workflow - 2026-06-12 ai-asset-regenerate-queued-job-followup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-asset-regenerate-queued-job-followup` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | admin-web AI 산출물 상세/재생성 연결 중 `상태 전이를 수행할 수 없습니다.` 원인 확인 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `doc/admin-server-sync-rules.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/**`, `docker-compose.yml`, `k8s/**` |

## 작업 목표

관리자 AI 산출물 재생성은 `POST /api/v1/admin/ai/assets/{assetId}/regenerate` 호출 시 새 산출물을 즉시 만드는 기능이 아니라 `ai_generation_jobs`에 `QUEUED` 작업을 등록하는 비동기 흐름이다. 현재 관리자 화면에서는 서버의 409 응답이 공통 메시지(`상태 전이를 수행할 수 없습니다.`)로만 보이고, 이미 같은 대상의 `QUEUED/RUNNING` 작업이 있는 경우와 산출물 상태가 재생성 불가인 경우를 구분하기 어렵다.

이 workflow는 재생성 실패/대기 상태를 관리자와 프론트 구현자가 명확히 판단할 수 있도록 서버 계약, 에러 메시지, 상세 조회 노출 여부, 로컬 워커 실행 정책을 정리하고 필요한 최소 구현 범위를 정의한다.

## 현재 확인된 사실

- `AiService.regenerateAiAsset`는 `REJECTED` 또는 `HIDDEN` 상태의 산출물만 재생성을 허용한다.
- 같은 `jobType + targetType + targetId`에 `QUEUED` 또는 `RUNNING` job이 있으면 중복 재생성을 차단한다.
- 차단 시 서버 HTTP 응답은 `409`, error code는 `C0007`, 화면 메시지는 `상태 전이를 수행할 수 없습니다.`로 보인다.
- 로컬 `docker-compose.yml` 기준 `qtai-admin-server`는 `AI_GENERATION_WORKER_ENABLED: "false"`라 재생성 job을 처리하지 않는다.
- 로컬 `service-ai`도 `AI_SCHEDULING_ENABLED=false` 기본값이라 스케줄러가 돌지 않는다.
- 따라서 로컬에서 재생성 요청이 성공해도 job은 `QUEUED`에 머물고, 같은 asset을 다시 누르면 중복 활성 job 때문에 409가 난다.

## 로컬 재현 증거

2026-06-12 로컬 DB에서 확인한 예시는 아래와 같다.

| assetId | asset status | target | active job | job status | 비고 |
| --- | --- | --- | --- | --- | --- |
| `31104` | `HIDDEN` | `BIBLE_VERSE #31104` | `31108` | `QUEUED` | 재생성 요청은 성공했으나 워커 비활성화로 대기 |
| `31102` | `REJECTED` | `BIBLE_VERSE #31102` | `31109` | `QUEUED` | 디버그 API 호출로 생성된 대기 job, 이후 화면 재요청은 409 |
| `4` | `APPROVED` | `BIBLE_VERSE #4` | 없음 | 해당 없음 | 상태 정책상 재생성 불가 |
| `2` | `VALIDATING` | `BIBLE_VERSE #2` | 없음 | 해당 없음 | 상태 정책상 재생성 불가 |

확인 쿼리:

```sql
SELECT a.id AS asset_id,
       a.status AS asset_status,
       a.target_type,
       a.target_id,
       j.id AS job_id,
       j.status AS job_status,
       j.active_unique_key,
       j.created_at,
       j.started_at,
       j.finished_at
FROM ai_generated_assets a
LEFT JOIN ai_generation_jobs j
  ON j.job_type = a.asset_type
 AND j.target_type = a.target_type
 AND j.target_id = a.target_id
WHERE a.id = 31102
ORDER BY j.id DESC
LIMIT 10;
```

## 범위

- 재생성 API의 현재 비동기 의미를 유지한다.
- 재생성 불가 사유를 서버/프론트가 구분할 수 있게 한다.
- 관리자 상세 조회에서 같은 target의 활성 generation job을 보여줄지 결정하고, 필요하면 응답 DTO를 확장한다.
- 로컬 개발에서 재생성 E2E를 검증할 때 어떤 워커를 켤지 운영 정책을 문서화한다.
- admin-server 복사본과 service-ai 원본 경계가 깨지지 않도록 `doc/admin-server-sync-rules.md`를 따른다.

## 제외 범위

- 평가셋/평가케이스 화면 구현.
- 평가 후보 등록 API 구현.
- DeepSeek 프롬프트/모델 품질 개선.
- 새 동기식 재생성 API 도입.
- 운영 compose/k8s 워커 토글 기본값 변경. 기본값 변경은 Lead 결정 후 별도 PR로 진행한다.
- 로컬 디버그로 생성된 `31108`, `31109` job 삭제 또는 DB 정리 자동화.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | 재생성 차단 사유가 구분되도록 예외 메시지/코드 정책 점검 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryService.java` | 상세 응답에 활성 job 정보를 포함할지 검토 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java` | target 기준 최신/활성 generation job 조회 필요 시 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/asset/dto/AdminAiAssetDetailResponse.java` | 활성 job 또는 pending regeneration 상태 필드 추가 여부 결정 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | 상태 불가와 활성 job 중복 차단을 각각 검증 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 409 응답 계약과 상세 응답 필드 검증 |
| Docs | `doc/workspaces/DevC_강상민/reports/**` | 구현/검증 결과 보고 |
| Coordination | `admin-web/src/pages/AiAssetsPage.tsx` | 프론트 담당이 `REJECTED/HIDDEN` 외 상태와 활성 job 표시를 반영할 수 있도록 계약 공유 |

## 구현 순서

1. 현재 API 계약을 재확인한다.
   - `GET /api/v1/admin/ai/assets/{assetId}`
   - `POST /api/v1/admin/ai/assets/{assetId}/regenerate`
2. `AiService.regenerateAiAsset`의 두 차단 조건을 테스트로 고정한다.
   - asset status가 `APPROVED`, `VALIDATING`, `NEEDS_REVIEW`이면 재생성 불가.
   - 같은 target에 `QUEUED` 또는 `RUNNING` job이 있으면 재생성 불가.
3. 409 응답에서 두 사유를 구분할 수 있는지 확인한다.
   - 현재 공통 `C0007`만 내려가면 프론트 표시용 메시지 보존 방식 또는 별도 error code가 필요한지 결정한다.
4. 관리자 상세 응답에 활성 job 정보를 추가할지 결정한다.
   - 권장 필드 후보: `activeGenerationJob` 또는 `pendingGenerationJob`.
   - 필드는 `id`, `jobType`, `targetType`, `targetId`, `promptVersionId`, `status`, `createdAt`, `startedAt`, `finishedAt`, `errorMessage` 정도로 제한한다.
5. 프론트 계약을 정리한다.
   - `REJECTED/HIDDEN` 외 상태에서는 재생성 버튼 비활성화.
   - 활성 job이 있으면 재생성 버튼 비활성화와 `이미 진행 중인 재생성 작업 #...` 표시.
   - 재생성 성공 시 “새 산출물 생성 완료”가 아니라 “재생성 작업 요청됨(QUEUED)”으로 표시.
6. 로컬 워커 실행 정책을 문서화한다.
   - 기본 compose에서는 중복 실행 방지를 위해 워커가 꺼져 있음을 명시한다.
   - E2E 검증 시에는 admin-server 또는 service-ai 중 하나만 켠다.
   - 기본값 변경은 이 workflow 범위에서 제외한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceTest` | `APPROVED` asset regenerate 요청 시 `INVALID_STATUS_TRANSITION` |
| `AiServiceTest` | `VALIDATING` asset regenerate 요청 시 `INVALID_STATUS_TRANSITION` |
| `AiServiceTest` | 같은 target에 `QUEUED` job이 있으면 regenerate 차단 |
| `AiServiceTest` | 같은 target에 `RUNNING` job이 있으면 regenerate 차단 |
| `AiServiceTest` | 같은 target의 `SUCCEEDED/FAILED` job은 새 regenerate를 막지 않음 |
| `AdminAiAssetControllerTest` | regenerate 409 응답의 error code/message 계약 |
| `AdminAiAssetControllerTest` | 상세 조회에 활성 job 필드 추가 시 `QUEUED` job 노출 |

## 수용 기준

- [ ] 재생성 가능 상태가 `REJECTED/HIDDEN`으로 명확히 고정된다.
- [ ] 활성 `QUEUED/RUNNING` job이 있으면 중복 재생성이 차단되는 테스트가 있다.
- [ ] 프론트가 “상태 불가”와 “이미 진행 중 job 존재”를 구분할 수 있는 계약이 있다.
- [ ] 상세 화면에서 활성 job 존재 여부를 알 수 있는 방식이 결정되어 있다.
- [ ] 로컬 E2E 검증 시 워커 토글을 어디서 켜야 하는지 문서화되어 있다.
- [ ] prompt 원문, provider raw response, secret/token/password/private key는 응답/로그/감사 snapshot에 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 후보가 `AiService`, asset detail query, controller response 계약에 강하게 연결되어 있어 한 작업자가 상태 전이와 응답 계약을 순서대로 고정하는 편이 안전하다.
- 워커 토글은 운영/Lead 결정과 맞물려 있어 병렬 구현보다 단일 판단으로 범위를 제한해야 한다.
- admin-web 반영은 별도 담당자가 계약 확정 후 import/표시만 맞추면 된다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 작업자가 서버 계약과 테스트를 먼저 확정하고, 이후 admin-web 담당자에게 응답 필드와 버튼 활성 조건을 공유한다.

## 검증 계획

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiServiceTest"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAiAssetControllerTest"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiGenerationJob*"
git diff --check
```

로컬 E2E는 워커 토글 정책 결정 후 아래 중 하나만 사용한다.

```powershell
# 선택 A: admin-server 워커만 임시 활성화
$env:AI_GENERATION_WORKER_ENABLED = "true"

# 선택 B: service-ai 스케줄링만 임시 활성화
$env:AI_SCHEDULING_ENABLED = "true"
```

두 토글을 동시에 켜면 같은 DB의 generation job을 두 프로세스가 처리할 수 있으므로 금지한다.

## 후속 작업으로 남길 항목

- admin-web `AiAssetsPage`에서 재생성 버튼을 `REJECTED/HIDDEN`이고 활성 job이 없을 때만 활성화한다.
- 재생성 성공 후 상세 drawer에 `QUEUED` job ID를 표시한다.
- AD-08 AI 배치 실행 로그와 asset 상세의 generation job 표시가 같은 의미를 쓰는지 문구를 맞춘다.
- 로컬 디버그로 남은 `QUEUED` job 정리는 필요 시 수동 DB 정리 또는 워커 임시 활성화로 처리한다.
