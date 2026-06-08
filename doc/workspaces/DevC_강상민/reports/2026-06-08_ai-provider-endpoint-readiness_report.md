# Report - 2026-06-08 ai-provider-endpoint-readiness

## 개요

- 작업명: `ai-provider-endpoint-readiness`
- 기준 workflow: `doc/workspaces/DevC_강상민/workflows/2026-06-08_ai-provider-endpoint-readiness.md`
- 작업 브랜치: `docs/ai-provider-endpoint-readiness`
- 실행 방식: 직접 실행
- 관련 F-ID: F-02, F-14, F-15
- 목적: provider endpoint가 열리기 전에 구현자가 AI outbound system endpoint 계약을 바로 확인할 수 있도록 readiness checklist를 작성했다.

## 변경 요약

- `doc/workspaces/DevC_강상민/2026-06-08_ai-provider-endpoint-readiness-checklist.md`를 추가했다.
- checklist에 공통 system endpoint 규약을 정리했다.
  - `/api/v1/system/**`
  - `Authorization: Bearer {service-token}` + `SYSTEM_BATCH`
  - `ApiResponse<T>` envelope
  - 선택적 `error.fields`
  - `traceparent` 전파와 응답 `traceId`
  - 쓰기 endpoint `Idempotency-Key`
  - `ApiResponse.error(code,message)`에서 AI `AiClientException`으로 변환
- endpoint별 provider, AI client, method, path, query, body, header, 응답 DTO를 정리했다.
  - QT context
  - 오늘 QT passage 상태
  - Bible verse 단건/목록/범위
  - Study publish/hide
  - Audit log
  - Admin/Auth active/verify/verify-any
- 후속 fixture 필수 케이스를 명시했다.
  - Today QT `STALE_FALLBACK`
  - Today QT `EMPTY`
  - provider `error.fields`
  - F-15 `blockedReason`
  - F-15 `blocked_reason`
  - malformed envelope

## 제외 범위 준수

- provider service Controller를 구현하지 않았다.
- AI HTTP adapter 코드를 변경하지 않았다.
- OpenAPI를 변경하지 않았다.
- 테스트 코드를 추가하거나 수정하지 않았다.
- DB schema, migration, seed를 변경하지 않았다.
- service-token 발급, JWKS, mTLS, gateway, Docker, Kubernetes 설정을 변경하지 않았다.
- Pact 또는 Spring Cloud Contract를 도입하지 않았다.

## 검증 결과

```powershell
git status --short --branch
```

- 결과: PASS
- 확인 내용: `docs/ai-provider-endpoint-readiness` 브랜치에서 문서 3개 변경만 존재

```powershell
git diff --name-only
```

- 결과: PASS
- 비고: report 작성 전에는 untracked 문서가 stage 전 상태라 출력이 없었고, 최종 stage 후에는 문서 3개만 확인한다.

```powershell
git diff --check
```

- 결과: PASS

```powershell
rg -n "<미완료 표시 문구>" <생성 문서 3개>
```

- 결과: PASS
- 확인 내용: 생성 문서에 미완료 표시 문구가 남아 있지 않음

## 후속 작업

- checklist 기준으로 request/response fixture를 별도 PR에서 정리한다.
- provider endpoint가 실제로 열리면 `mode=http` 기반 provider smoke test를 별도 PR에서 진행한다.
