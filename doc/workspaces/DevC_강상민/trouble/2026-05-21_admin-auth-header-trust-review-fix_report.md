# Report — 2026-05-21 admin-auth-header-trust-review-fix

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 관련 PR | `feat(ai): AI 생성 작업 큐잉과 관리자 재생성 요청 추가 #40` |
| 실행 경로 | REQUEST_CHANGES 보안 리뷰 대응 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-05-21_admin-auth-header-trust-review-fix_report.md` |

## 증상

PR 리뷰에서 `REQUEST_CHANGES`를 받았다. 관리자 AI 산출물 재생성 API가 `X-Admin-Id`, `X-Member-Role`, `X-Admin-Role` 같은 HTTP 헤더 값을 신뢰하고 있어, 요청자가 헤더를 조작하면 관리자 권한처럼 보일 수 있는 보안 문제가 지적되었다.

## 원인

관리자 식별자와 권한 정보는 클라이언트가 보내는 값이 아니라 서버가 검증한 인증 정보에서 가져와야 한다. 하지만 기존 구현에서는 `AdminAiAssetController`가 `@RequestHeader`로 관리자 ID와 역할을 직접 받아 `RegenerateAiAssetCommand`에 전달했다.

이 구조에서는 JWT 검증이나 Spring Security 인증 결과와 무관하게 클라이언트가 임의의 권한 값을 보낼 수 있다. 특히 `X-Member-Role=ADMIN`, `X-Admin-Role=REVIEWER` 같은 헤더를 직접 넣으면 UseCase가 관리자 요청처럼 판단할 위험이 있었다.

## 해결

헤더 기반 권한 전달을 제거하고, Spring Security의 `Authentication` 또는 `SecurityContextHolder`에서 인증 메타데이터를 추출하도록 수정했다.

인증되지 않은 요청은 `UNAUTHORIZED`, 관리자 회원 역할이 없거나 세부 관리자 권한이 부족한 요청은 `FORBIDDEN`으로 차단한다. 권한 검증이 실패하면 `RegenerateAiAssetUseCase`를 호출하지 않는다.

## 변경 요약

1. `AdminAiAssetController.regenerate(...)`에서 `@RequestHeader("X-Admin-Id")`, `@RequestHeader("X-Member-Role")`, `@RequestHeader("X-Admin-Role")` 파라미터를 제거했다.
2. Controller 메서드가 `Authentication`을 직접 받도록 바꿨다.
3. `Authentication`이 없거나 익명 인증이면 `BusinessException(ErrorCode.UNAUTHORIZED)`를 던진다.
4. `ROLE_ADMIN` 또는 `MEMBER_ROLE_ADMIN` 권한이 없으면 `FORBIDDEN`으로 차단한다.
5. `ADMIN_ROLE_REVIEWER` 또는 `ADMIN_ROLE_SUPER_ADMIN` 권한이 없으면 `FORBIDDEN`으로 차단한다.
6. 인증 principal에서 관리자 ID를 추출하고, 숫자로 해석할 수 없으면 `UNAUTHORIZED`로 차단한다.
7. 위조 헤더가 있어도 SecurityContext 권한이 부족하면 UseCase가 호출되지 않도록 테스트를 추가했다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | 관리자 식별·권한 추출을 HTTP 헤더에서 SecurityContext 기반으로 변경 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | 위조 헤더 차단, 권한 부족 차단, UseCase 미호출 negative test 추가 |

## 테스트 보강

| 테스트 | 검증 내용 |
| --- | --- |
| `regenerateMapsRequestAndReturnsAcceptedQueuedJob` | 인증된 `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 요청만 command로 매핑되는지 검증 |
| `forbiddenAdminRoleReturnsForbiddenResponse` | 관리자 세부 권한이 부족하면 `403`을 반환하고 UseCase를 호출하지 않는지 검증 |
| `unauthenticatedRequestReturnsUnauthorizedEvenWithForgedHeaders` | 인증 없이 위조 권한 헤더만 보내도 `401`로 차단되는지 검증 |
| `forgedHeadersDoNotOverrideSecurityContextRoles` | SecurityContext가 `ROLE_USER`이면 위조 헤더가 있어도 `403`으로 차단되는지 검증 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle -p qtai-server test --tests "*AdminAiAssetControllerTest"` | 통과 |
| `gradle -p qtai-server clean build --no-build-cache` | 통과 |
| `Get-ChildItem ... | Select-String 'X-Admin-Id','X-Member-Role','X-Admin-Role','@RequestHeader(\"X-'` | main AI 코드에서 권한 헤더 기반 인증 잔존 없음 |

저장소 루트와 `qtai-server`에 `gradlew`가 없고 시스템 `gradle` 명령도 설치되어 있지 않아, `%TEMP%/codex-gradle/gradle-8.10.2` 임시 Gradle 배포본으로 검증했다.

## 배운 점

관리자 ID, 회원 역할, 관리자 세부 권한은 HTTP header나 body처럼 사용자가 조작할 수 있는 입력값을 신뢰하면 안 된다. 권한 판단은 반드시 JWT 검증 이후 서버가 구성한 `SecurityContext` 같은 인증 컨텍스트를 기준으로 해야 한다.

이번 PR에서는 인증 인프라가 아직 완전히 확정되지 않아 Controller 내부에서 최소한의 SecurityContext 기반 추출을 구현했다. 후속으로 관리자 인증 principal 구조가 확정되면 공통 security adapter로 분리하는 것이 적절하다.
