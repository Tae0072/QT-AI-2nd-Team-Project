# Report — 2026-05-21 admin-ai-generation-trigger-second-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 관련 PR | `feat(ai): AI 생성 작업 큐잉과 관리자 재생성 요청 추가 #40` |
| 실행 경로 | 2차 PR 리뷰 대응 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/trouble/2026-05-21_admin-ai-generation-trigger-second-review-fixes_report.md` |

## 증상

2차 PR 리뷰에서 관리자 AI 재생성 트리거 API의 코드 품질, 예외 처리, 보안, 멱등성, API envelope, 문서 중복에 대한 추가 지적을 받았다.

핵심 지적은 다음 세 가지였다.

1. `AiService.jobTypeOf(AiGeneratedAssetType)`가 enum 이름 일치에 의존해 새 enum 추가 시 500으로 전파될 수 있다.
2. `AdminAiAssetController`가 권한 문자열을 여러 prefix 형태로 허용해 인증 인프라 확정 전 공격면이 넓다.
3. `ApiResponse`의 `traceId` 필드는 추가됐지만 MDC에 들어온 `traceId`가 그대로 응답에 전파되는지 테스트가 없다.

리뷰에서 함께 언급된 `active_unique_key` 운영 DB migration과 `AI_REGENERATE_REQUEST` 감사 로그는 현재 저장소에 migration 도구와 audit write 계약 구현이 아직 없어, 본 PR에서 코드로 완결하기 어렵다. 따라서 이번 대응에서는 즉시 코드로 줄일 수 있는 리스크를 먼저 보강하고, migration/audit은 PR 본문에 명확한 sequencing 약속을 남기는 방향으로 정리했다.

## 원인

`jobTypeOf`는 `AiGeneratedAssetType`과 `AiGenerationJobType`의 enum 이름이 대부분 같다는 전제에 기대고 있었다. `QA_RESPONSE -> QA`만 예외 처리하고 나머지는 `AiGenerationJobType.valueOf(assetType.name())`로 처리했기 때문에, 새 asset type이 추가되면 컴파일 단계가 아니라 런타임에 실패할 수 있었다.

관리자 권한 검증은 `ADMIN`, `ROLE_ADMIN`, `MEMBER_ROLE_ADMIN`과 `REVIEWER`, `ROLE_REVIEWER`, `ADMIN_ROLE_REVIEWER` 형태를 모두 허용했다. 인증 인프라가 확정되지 않은 상황을 흡수하려는 의도였지만, 리뷰 기준에서는 허용 범위가 너무 넓었다.

`ApiResponse`는 MDC `traceId`를 우선 사용하고 없으면 UUID를 생성하도록 구현했지만, 테스트는 `traceId` 존재 여부만 확인했다. MDC 분기가 미커버라 실제 요청 추적값 전파가 깨져도 테스트가 잡지 못했다.

## 해결

즉시 수정 가능한 항목은 코드와 테스트로 반영했다.

1. `AiService.jobTypeOf(...)`를 명시 `switch` 매핑으로 바꿨다.
2. `regenerateAiAsset`의 권한 재검증은 Controller 우회와 내부 호출 오용을 막기 위한 2차 검증이라는 주석을 추가했다.
3. `AdminAiAssetController`의 권한 prefix 허용을 `ROLE_ADMIN`, `ADMIN_ROLE_REVIEWER`, `ADMIN_ROLE_SUPER_ADMIN`으로 축소했다.
4. `AdminAiAssetControllerTest`에 MDC `traceId`가 응답 envelope에 그대로 들어가는지 검증하는 테스트를 추가했다.

## 변경 요약

| 파일 | 변경 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | `jobTypeOf`를 enum name fallback에서 명시 switch 매핑으로 변경 |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | `requireAuthorizedReviewer` 호출 앞에 2차 권한 검증 의도 주석 추가 |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiAssetController.java` | 관리자 권한 허용 prefix를 `ROLE_ADMIN`, `ADMIN_ROLE_*`로 축소 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | MDC `traceId` 전파 테스트와 `MDC.clear()` 정리 추가 |

## 리뷰 지적별 대응

| 리뷰 지적 | 대응 |
| --- | --- |
| `jobTypeOf` enum name fallback은 새 enum 추가 시 500 가능 | 명시 switch로 변경해 enum 추가 시 컴파일 단계에서 누락을 드러내도록 수정 |
| Controller와 Service의 권한 검증 중복 의도 불명확 | 도메인 경계의 2차 권한 검증이라는 주석 추가 |
| 권한 prefix 허용 범위가 넓음 | `ROLE_ADMIN`, `ADMIN_ROLE_REVIEWER`, `ADMIN_ROLE_SUPER_ADMIN`만 허용 |
| MDC traceId 분기 테스트 없음 | `responseEnvelopeUsesMdcTraceIdWhenPresent` 테스트 추가 |
| active_unique_key 운영 DB migration 없음 | 현재 repo에 migration 도구가 없어 본 PR에서 안전하게 추가 불가. PR 본문에 migration 선행 또는 후속 sequencing 명시 필요 |
| 감사 로그가 TODO만 있음 | `WriteAuditLogUseCase`가 메서드 없는 TODO 포트라 본 PR에서 호출 불가. audit 계약 확정 후 후속 PR로 연결 필요 |

## 테스트 보강

| 테스트 | 검증 내용 |
| --- | --- |
| `responseEnvelopeUsesMdcTraceIdWhenPresent` | MDC에 `traceId`가 있으면 `ApiResponse.traceId`가 같은 값으로 내려가는지 검증 |
| 기존 `AdminAiAssetControllerTest` | prefix 축소 이후에도 `ROLE_ADMIN + ADMIN_ROLE_REVIEWER` 정상 요청, 권한 부족 차단 테스트 유지 |
| 기존 `AiServiceTest` | `jobTypeOf` 명시 매핑 변경 이후 관리자 재생성 job 생성 흐름 유지 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle -p qtai-server test --tests "*AdminAiAssetControllerTest" --tests "*AiServiceTest" --tests "*AiGenerationJobTest"` | 통과 |
| `gradle -p qtai-server clean build --no-build-cache` | 통과 |
| `git diff --check` | 공백 오류 없음. CRLF 변환 경고만 출력됨 |
| `Get-ChildItem ... | Select-String 'X-Admin-Id','X-Member-Role','X-Admin-Role','@RequestHeader(\"X-'` | main AI 코드에서 권한 헤더 기반 인증 잔존 없음 |
| `rg -n "String\\.valueOf\\(command\\.promptVersionId\\)\|AiGenerationJobType\\.valueOf\\(assetType\\.name\\(\\)\\)" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |

저장소 루트와 `qtai-server`에 `gradlew`가 없고 시스템 `gradle` 명령도 설치되어 있지 않아, `%TEMP%/codex-gradle/gradle-8.10.2` 임시 Gradle 배포본으로 검증했다.

## 머지 전 본문 명시 필요 항목

아래 두 항목은 이번 PR에서 코드로 완결하지 못했다. 리뷰어가 머지 조건으로 보는 경우 PR 본문에 명확히 적어야 한다.

```markdown
## 머지 전/후속 순서 확인
- DB migration: `ai_generation_jobs.active_unique_key` 컬럼과 `uk_ai_generation_jobs_active_target_prompt` unique index는 DB migration 인프라 확정 PR에서 먼저 반영한다. 현재 PR의 JPA unique constraint는 도메인 모델 기준이며 운영 MySQL 반영은 별도 migration이 필요하다.
- Audit: `AI_REGENERATE_REQUEST` 감사 로그는 `domain.audit.api.WriteAuditLogUseCase` 계약 확정 후 후속 PR에서 연결한다. 현재 audit 포트는 메서드가 없는 TODO 상태라 본 PR에서 안전하게 호출할 수 없다.
```

## 배운 점

enum 간 매핑은 이름이 같아 보이더라도 명시적으로 작성하는 편이 안전하다. 이름 기반 fallback은 지금은 편하지만 새 enum이 추가될 때 런타임 500으로 터질 수 있다.

권한 문자열도 인증 인프라가 확정되기 전이라고 해서 여러 형태를 넓게 허용하면 보안 리뷰에서 공격면으로 해석된다. 임시 구현이라도 허용 prefix는 좁게 잡고, 실제 principal/authority 표준이 확정되면 공통 security adapter로 옮기는 편이 낫다.

응답 envelope의 `traceId`는 필드 존재만으로 충분하지 않다. 실제 요청 단위 추적값이 전파되는지 확인해야 운영 로그와 클라이언트 오류 리포트가 같은 요청을 가리킬 수 있다.
