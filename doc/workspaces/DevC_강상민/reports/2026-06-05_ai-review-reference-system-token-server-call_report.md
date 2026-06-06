# Report - 2026-06-05 ai-review-reference-system-token-server-call

## 개요

- 브랜치: `test/ai-reference-system-token-server-call`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-system-token-server-call.md`
- 목표 URI: `restricted://validation/index/reference-index.json`
- 목적: 실제 random port 서버 호출에서 JWT `role=SYSTEM_BATCH` token이 system API를 통과하고 최종 URI가 DB와 latest metadata로 이어지는지 검증

## 변경 요약

- `AiReviewReferenceSystemTokenServerCallIntegrationTest`
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` 기반 실제 HTTP 호출 검증 추가
  - `JwtProvider.issueAccessToken(1L, "SYSTEM_BATCH")`로 Bearer token 발급
  - 정상 POST 호출의 `201 Created`, 응답 민감 URI/hash 미노출, DB 저장, latest metadata 조회 검증
  - 토큰 없음, 변조 토큰, `role=USER` token 실패 응답 검증
- 생산 코드, DB schema, OpenAPI, API 응답 필드는 변경하지 않음

## 검증 결과

### Focused test

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceSystemTokenServerCallIntegrationTest" --tests "*JwtProviderTest" --tests "*SystemValidationReferenceJobControllerTest"
```

결과: `BUILD SUCCESSFUL`

### 흐름 검증

- JWT `role=SYSTEM_BATCH` token 발급: 통과
- 실제 `JwtAuthenticationFilter` 인증: 통과
- system API 정상 POST: 통과
- API 응답 민감 URI/hash 미노출: 통과
- DB row 최종 URI 저장: 통과
- latest ACTIVE metadata 최종 URI 반환: 통과
- 토큰 없음 401: 통과
- 변조 token 401: 통과
- `role=USER` token 403: 통과

## 안전 확인

### 민감 산출물 ignored 상태

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
```

결과:

```text
!! doc/TalkFile_IVP성경배경주석.pdf.pdf
!! qtai-server/build/
!! qtai-server/restricted/
```

### 민감 산출물 staged 제외

```powershell
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
```

결과: 출력 없음

### report 안전 키워드 확인

지정된 안전 검색 명령 실행 결과: 출력 없음

## 비고

인증 실패 케이스는 실제 서버의 system GET endpoint로 검증했다. POST body가 있는 401 케이스는 JDK HTTP client streaming retry 제약에 걸릴 수 있어, token 인증/인가 실패 자체는 body 없는 system endpoint에서 고정했다.

이번 작업의 system token은 현재 구현 계약인 JWT `role=SYSTEM_BATCH` access token이다. 전용 service account/shared-secret/mTLS 인증은 후속 작업으로 남긴다.
