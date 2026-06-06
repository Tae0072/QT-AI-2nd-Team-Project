# Workflow - 2026-06-05 ai-review-reference-system-token-server-call

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-reference-system-token-server-call` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `POST /api/v1/system/validation-reference-jobs`를 실제 random port 서버 호출로 검증하고, JWT `role=SYSTEM_BATCH` token이 system API를 통과하는지 확인해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-validation-reference-jobs.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-system-api-index-storage-uri-flow.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

실제 Spring Boot random port 서버 호출로 `POST /api/v1/system/validation-reference-jobs`가 JWT `role=SYSTEM_BATCH` access token을 인증/인가하고, 최종 `indexStorageUri = restricted://validation/index/reference-index.json`를 DB에 저장하는지 검증한다.

이번 작업의 system token은 현재 구현/문서 계약인 JWT `role=SYSTEM_BATCH` access token으로 정의한다. 별도 service account, shared-secret, mTLS, 전용 token 발급 API는 구현하지 않는다.

## 범위

- random port 서버 호출 기반 통합 테스트를 추가한다.
- `JwtProvider.issueAccessToken(1L, "SYSTEM_BATCH")`로 발급한 Bearer token이 실제 `JwtAuthenticationFilter`를 통과하는지 검증한다.
- 정상 호출은 `201 Created`, DB 저장, latest ACTIVE metadata 조회까지 검증한다.
- 토큰 없음, 변조 토큰, `role=USER` 토큰 실패 응답을 검증한다.
- workflow/report를 작성한다.

## 제외 범위

- 생산 코드 변경
- DB schema, OpenAPI, API 응답 필드 변경
- service account/shared-secret/system-token filter 구현
- 실제 운영 server 또는 curl 수동 검증
- 운영 restricted storage 배포
- 실제 IVP 원문 JSON, PDF, `qtai-server/restricted/**`, `qtai-server/build/**` 커밋

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceSystemTokenServerCallIntegrationTest.java` | random port 실제 HTTP 호출과 JWT system token 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-system-token-server-call.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-system-token-server-call_report.md` | 실행 결과와 검증 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-reference-system-token-server-call` 브랜치를 생성한다.
2. workflow spec을 저장한다.
3. `AiReviewReferenceSystemTokenServerCallIntegrationTest`를 추가한다.
4. 신규 테스트는 `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, `TestRestTemplate`을 사용한다.
5. 각 테스트 시작 전 `ValidationReferenceJobRepository.deleteAll()`와 `flush()`로 테스트 데이터를 정리한다.
6. 정상 POST는 `Authorization: Bearer {JwtProvider.issueAccessToken(1L, "SYSTEM_BATCH")}`를 사용한다.
7. 정상 POST 응답에서 `sourceFileHash`, `storageUri`, `indexStorageUri`가 노출되지 않는지 검증한다.
8. POST 응답 id로 DB row를 조회해 source hash, storage URI, index URI, ACTIVE 상태를 검증한다.
9. `AiReviewReferenceService.latestActiveReference()`가 같은 id, source hash, index URI를 반환하는지 검증한다.
10. 토큰 없음, 변조 토큰, `role=USER` 토큰 요청 실패를 검증한다.
11. focused test를 실행한다.
12. 민감 산출물 ignored/staged 상태를 확인한다.
13. report를 작성하고 원문성 키워드가 없는지 확인한다.
14. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceSystemTokenServerCallIntegrationTest` | JWT `role=SYSTEM_BATCH` token으로 실제 server POST 호출 성공 |
| `AiReviewReferenceSystemTokenServerCallIntegrationTest` | API 응답에는 민감 URI/hash가 노출되지 않음 |
| `AiReviewReferenceSystemTokenServerCallIntegrationTest` | DB row에 최종 URI와 source hash가 저장됨 |
| `AiReviewReferenceSystemTokenServerCallIntegrationTest` | latest ACTIVE reference metadata가 최종 URI를 반환 |
| `AiReviewReferenceSystemTokenServerCallIntegrationTest` | 토큰 없음 401, 변조 토큰 401, `role=USER` token 403 |

## 수용 기준

- [ ] 실제 random port HTTP POST가 JWT `role=SYSTEM_BATCH` token으로 `201 Created`를 반환한다.
- [ ] 실제 `JwtAuthenticationFilter`가 발급 token을 인증하고 `ROLE_SYSTEM_BATCH` authority를 만든다.
- [ ] API 응답에는 `sourceFileHash`, `storageUri`, `indexStorageUri`가 노출되지 않는다.
- [ ] DB row에 `restricted://validation/index/reference-index.json`가 저장된다.
- [ ] latest ACTIVE reference metadata가 같은 id, source hash, index URI를 반환한다.
- [ ] 토큰 없음은 401, 변조 토큰은 401, `role=USER` token은 403이다.
- [ ] 생산 코드, DB schema, OpenAPI, API 응답 필드는 변경되지 않는다.
- [ ] focused test가 통과한다.
- [ ] 민감 산출물은 ignored 상태이며 staged 대상이 아니다.
- [ ] report에는 원문성 키워드가 포함되지 않는다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 단일 통합 테스트와 문서에 한정되어 있다.
- 인증, HTTP 호출, DB 저장, metadata 조회가 같은 테스트 흐름에 묶여 있다.
- 민감 산출물 stage 금지 확인을 마지막에 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 테스트 보강, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceSystemTokenServerCallIntegrationTest" --tests "*JwtProviderTest" --tests "*SystemValidationReferenceJobControllerTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "referenceText|excerpt|본문" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-review-reference-system-token-server-call_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 전용 `service_accounts` 기반 system token 또는 shared-secret 인증 구현
- 실제 운영 server/curl 수동 검증
- 운영 환경 `QTAI_RESTRICTED_STORAGE_ROOT` 연결 검증
