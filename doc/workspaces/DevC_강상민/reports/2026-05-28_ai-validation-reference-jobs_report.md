# 2026-05-28 ai-validation-reference-jobs 작업 보고

## 개요

- 관련 F-ID: F-14
- workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-validation-reference-jobs.md`
- 브랜치: `feature/ai-validation-reference-jobs`
- PR 대상: `dev`

## 변경 내용

- `POST /api/v1/system/validation-reference-jobs` 생성 API 추가
- `GET /api/v1/system/validation-reference-jobs/{jobId}` 단건 조회 API 추가
- `POST /api/v1/system/validation-reference-jobs/{jobId}/expire` 만료 API 추가
- `validation_reference_jobs` entity, status enum, repository, service 구현
- 생성/만료 감사 로그 기록 추가
- OpenAPI와 `04_API_명세서.md` 7.4를 SYSTEM_BATCH 기준 계약으로 정리

## 계약 결정 반영

- 직접 호출 권한은 `SYSTEM_BATCH` 또는 `ROLE_SYSTEM_BATCH`만 허용한다.
- `ROLE_ADMIN + ADMIN_ROLE_CONTENT_CREATOR` 직접 호출은 `403 FORBIDDEN`으로 차단한다.
- 생성 시 상태는 항상 `ACTIVE`이고, 요청자가 상태를 지정할 수 없다.
- 만료는 `ACTIVE -> EXPIRED`만 허용하며 `deletedAt`은 설정하지 않는다.
- 응답 DTO에는 `id`, `sourceName`, `sourceFileName`, `status`, `expiresAt`, `deletedAt`, `createdAt`, `updatedAt`만 포함한다.
- 내부 저장 위치와 해시 값은 저장하되 기본 응답과 감사 snapshot에는 포함하지 않는다.

## 검증 결과

- `.\qtai-server\gradlew.bat -p qtai-server test --tests "*ValidationReferenceJob*"`: 통과
- `.\qtai-server\gradlew.bat -p qtai-server test --tests "com.qtai.domain.ai.api.AiUseCaseContractTest"`: 통과
- `.\qtai-server\gradlew.bat -p qtai-server test --tests "com.qtai.domain.ai.web.SystemValidationReferenceJobControllerTest" --tests "com.qtai.domain.ai.internal.ValidationReferenceJobServiceTest" --tests "com.qtai.domain.ai.internal.ValidationReferenceJobRepositoryTest" --tests "com.qtai.domain.ai.internal.ValidationReferenceJobTest"`: 통과
- `.\qtai-server\gradlew.bat -p qtai-server build`: 통과
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`: PowerShell 실행 정책으로 `npx.ps1` 실행 차단
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`: `.spectral.yaml` 파일 부재로 실패
- validation reference jobs 경로 확인: 사용자 앱 경로와 일반 관리자 경로에 신규 API 노출 없음
- 금지 본문/민감 문자열 확인: 신규 report와 OpenAPI에는 매치 없음. 테스트에는 감사 snapshot 미포함을 검증하는 부정 assertion만 매치됨
- `.\qtai-server\gradlew.bat -p qtai-server jacocoTestReport --dry-run`: `jacocoTestReport` task 부재로 실패
- `.\qtai-server\gradlew.bat -p qtai-server jacocoTestCoverageVerification --dry-run`: `jacocoTestCoverageVerification` task 부재로 실패
- `gitleaks version`: 로컬에 gitleaks 실행 파일이 없어 실행 불가
- `git diff --check`: 공백 오류 없음. 기존/수정 파일의 CRLF 경고만 출력됨

## 제외 범위

- 목록 조회 API
- CONTENT_CREATOR 직접 접근 권한
- service account credential 저장 방식과 `/api/v1/system/**` 전역 보안 필터
- 실제 임시 파일/색인 삭제와 `DELETED` 전이 API
- 영구 주석 자료 관리 API
- AI 생성/검증 실행, DeepSeek 호출, 관리자 승인/반려 흐름 변경

## 잔여 리스크와 후속 작업

- Spectral ruleset 파일이 없어 OpenAPI lint를 완료하지 못했다.
- 시스템 계정 id가 아직 없어 감사 로그 `actorId`는 null로 남긴다.
- 실제 파일/색인 삭제 배치가 없으므로 `EXPIRED` 이후 정리 흐름은 별도 workflow가 필요하다.
