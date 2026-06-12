# Report - 2026-06-12 ai-asset-regenerate-queued-job-followup

## 작업 정보

| 항목 | 내용 |
| --- | --- |
| 유형 | 원인 조사 / 후속 작업 인계 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-12_ai-asset-regenerate-queued-job-followup.md` |
| 관련 화면 | admin-web `/ai-assets` |
| 관련 API | `GET /api/v1/admin/ai/assets/{assetId}`, `POST /api/v1/admin/ai/assets/{assetId}/regenerate` |
| 관련 F-ID | F-14 |
| 코드 변경 | 없음 |

## 요약

admin-web AI 산출물 상세 화면에서 재생성 요청 시 `상태 전이를 수행할 수 없습니다.` 토스트가 뜨는 원인을 확인했다. 권한 문제는 아니며, `SUPER_ADMIN` 토큰으로 API 호출은 정상 도달한다.

원인은 두 가지로 분리된다.

1. 서버 정책상 재생성은 `REJECTED` 또는 `HIDDEN` 상태의 asset만 허용한다.
2. 같은 `jobType + targetType + targetId`에 `QUEUED` 또는 `RUNNING` generation job이 있으면 중복 재생성을 차단한다.

현재 로컬 compose는 AI generation worker가 비활성화되어 있어, 성공한 재생성 요청도 `QUEUED` 상태에 머문다. 따라서 같은 asset을 다시 요청하면 활성 job이 이미 존재해 409가 발생한다.

## 확인 결과

### 1. 상태 정책

`AiService.regenerateAiAsset`는 재생성 대상 asset의 상태를 먼저 확인한다.

- 허용: `REJECTED`, `HIDDEN`
- 차단: `APPROVED`, `VALIDATING`, `NEEDS_REVIEW` 등

직접 API 확인 결과:

| assetId | 상태 | 결과 |
| --- | --- | --- |
| `4` | `APPROVED` | `409 C0007` |
| `2` | `VALIDATING` | `409 C0007` |
| `31102` | `REJECTED` | `200`, generation job `31109` 생성 |

### 2. 활성 job 중복 차단

같은 target에 `QUEUED` 또는 `RUNNING` job이 있으면 서버가 중복 재생성을 막는다. 로컬 DB 기준 확인된 상태는 아래와 같다.

| assetId | asset status | target | active job | job status | 비고 |
| --- | --- | --- | --- | --- | --- |
| `31104` | `HIDDEN` | `BIBLE_VERSE #31104` | `31108` | `QUEUED` | 기존 재생성 요청으로 생성됨 |
| `31102` | `REJECTED` | `BIBLE_VERSE #31102` | `31109` | `QUEUED` | 이번 조사 중 직접 API 호출로 생성됨 |

`31102`에서 화면 재요청 시 409가 난 이유는 DB에 `31109`가 이미 `QUEUED`로 존재하기 때문이다.

### 3. 워커 비활성화

로컬 실행 환경에서 generation job을 처리할 스케줄러가 꺼져 있다.

- `qtai-admin-server`: `AI_GENERATION_WORKER_ENABLED=false`
- `qtai-service-ai`: `AI_SCHEDULING_ENABLED=false`
- `docker-compose.yml`도 동일한 기본값을 사용한다.

따라서 재생성 요청은 job 생성까지 성공하지만, 새 asset 생성/검증 단계로 진행되지 않는다.

## 재현/확인 명령

DB 확인:

```powershell
$pw = docker exec qtai-mysql printenv MYSQL_PASSWORD
docker exec qtai-mysql mysql -uqtai "-p$pw" qtai -e "
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
WHERE a.id IN (31102, 31104)
ORDER BY a.id, j.id DESC;"
```

워커 토글 확인:

```powershell
docker exec qtai-admin-server printenv | Select-String -Pattern "AI_GENERATION|WORKER|SPRING_PROFILES"
docker exec qtai-service-ai printenv | Select-String -Pattern "AI_SCHEDULING|SPRING_PROFILES"
```

## 화면 영향

현재 admin-web은 상세 drawer에서 asset 상태와 활성 job 존재 여부를 기준으로 재생성 버튼을 제한하지 않는다. 그래서 사용자는 다음 상황을 모두 같은 실패처럼 보게 된다.

- `APPROVED/VALIDATING`이라 서버 정책상 재생성 불가
- `REJECTED/HIDDEN`이지만 이미 `QUEUED` job이 있어 중복 요청 불가
- 재생성 요청은 성공했지만 워커가 꺼져 완료 asset이 생성되지 않음

서버 응답도 현재 공통 `C0007` 메시지로 내려와 프론트에서 세부 사유를 알기 어렵다.

## 권장 후속 조치

### 서버

- `AiServiceTest`에 상태 불가와 활성 job 중복 차단 케이스를 명시적으로 추가한다.
- 프론트가 구분 가능한 재생성 차단 계약을 정한다.
  - 옵션 A: 기존 `C0007` 유지, message를 사유별로 보존해 내려준다.
  - 옵션 B: 별도 error code를 추가한다.
- `GET /admin/ai/assets/{assetId}` 상세 응답에 같은 target의 활성 generation job 정보를 포함할지 결정한다.
  - 후보 필드: `activeGenerationJob` 또는 `pendingGenerationJob`

### 프론트

- `REJECTED/HIDDEN`이 아닌 asset은 재생성 버튼을 비활성화한다.
- 상세 응답에서 활성 job이 확인되면 재생성 버튼을 비활성화하고 job ID/status를 표시한다.
- 재생성 성공 문구는 “산출물 생성 완료”가 아니라 “재생성 작업 요청됨(QUEUED)”으로 유지한다.

### 로컬 운영

- 로컬 E2E 검증 시에는 admin-server 또는 service-ai 중 하나의 워커만 켠다.
- 두 워커를 동시에 켜지 않는다. 같은 DB의 generation job을 두 프로세스가 처리할 수 있다.
- 기본 compose 토글 변경은 Lead 결정 후 별도 PR로 분리한다.

## 검증 결과

| 확인 항목 | 결과 |
| --- | --- |
| 브라우저 콘솔 에러 | 재생성 실패와 직접 관련된 에러 없음 |
| `SUPER_ADMIN` API 권한 | 호출 가능 |
| `APPROVED` asset 재생성 | `409 C0007` |
| `VALIDATING` asset 재생성 | `409 C0007` |
| `REJECTED` asset 재생성 | `200`, `QUEUED` job 생성 |
| 활성 job 존재 시 재요청 | `409 C0007` |
| admin-server worker | 비활성화 |
| service-ai scheduling | 비활성화 |

## 주의 사항

- 조사 중 `assetId=31102`에 대해 직접 API 호출을 수행하여 `ai_generation_jobs.id=31109`가 생성되었다.
- `31108`, `31109`는 로컬 DB의 디버그/검증 흔적이다. 후속 화면 테스트에서 같은 asset 재생성이 계속 막히면 이 `QUEUED` job이 원인일 수 있다.
- 이 리포트는 원인 조사 결과이며 서버 코드 수정과 테스트 추가는 아직 수행하지 않았다.

## 결론

현재 재생성 실패는 DB에 같은 target의 활성 job이 이미 있는 경우와 서버 상태 정책상 재생성 불가인 경우가 섞여 보이는 문제다. 로컬에서는 워커가 꺼져 있어 `QUEUED` job이 해소되지 않으므로, 한 번 성공한 재생성 요청 이후 같은 asset은 계속 409로 막힌다.

강상민 후속 작업은 서버 계약을 먼저 확정하고, 이후 admin-web에서 버튼 활성 조건과 pending job 표시를 맞추는 순서가 안전하다.
