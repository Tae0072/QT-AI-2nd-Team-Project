# 워크플로우 — 관리자 미션 관리(AD-16) 복원

- 날짜: 2026-06-15
- 작성: Lead 강태오 (with Claude)
- 브랜치: `feature/admin-mission-management` → PR to `dev`
- 기능: F-13 미션(Mission) 관리 — 관리자 웹에서 미션 정의 CRUD

## 1. 배경

이전 작업 중 `git pull`이 막혀 `stash -u`로 백업해 둔 미완성 WIP 4건(미션 관리·앱 버전·셀프테스트·나눔 공유글 관리) 중 **미션 관리**를 첫 번째로 복원한다. 4건 모두 현재 `dev`에는 없는 것으로 확인했고, 충돌·스코프 혼입을 피하기 위해 **기능별 1 PR**로 잘게 나눠 복원한다.

## 2. 한 일 (요약)

관리자 웹에 "미션 관리(AD-16)" 화면을 추가하고, admin-server에 미션 정의 CRUD API를 붙였다. 미션 **정의**(카탈로그)만 다루며, 회원별 **진행률 집계**는 건드리지 않는다(정의/진행 분리 원칙 유지).

### 백엔드 (admin-server, `domain.mission`)

- `mission/api/AdminMissionUseCase.java` — 외부 공개 UseCase 인터페이스(list/get/create/update/changeStatus)
- `mission/api/dto/AdminMissionResponse.java`, `MissionCreateRequest.java`, `MissionUpdateRequest.java` — 요청/응답 DTO(record)
- `mission/internal/AdminMissionService.java` — UseCase 구현. 생성 시 `code` 중복 검사, 수정은 null 인자 부분수정, 상태는 ACTIVE/HIDDEN만 허용
- `mission/web/AdminMissionController.java` — `/api/v1/admin/missions` REST. `ROLE_ADMIN` + `CONTENT_CREATOR`/`OPERATOR` 인가(회원관리 컨트롤러와 동일 패턴)
- `mission/internal/MissionDefinition.java` (수정) — 빌더에 `updatedAt=createdAt` 초기화, `update(...)`/`changeStatus(...)` 도메인 메서드 추가
- `mission/internal/MissionDefinitionRepository.java` (수정) — `findAllByOrderByIdAsc()`, `existsByCode(String)` 추가

엔드포인트:

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/admin/missions` | 전체 목록(id 오름차순) |
| GET | `/api/v1/admin/missions/{id}` | 단건 상세 |
| POST | `/api/v1/admin/missions` | 생성(code 중복 검사) |
| PATCH | `/api/v1/admin/missions/{id}` | 부분 수정(code 불변) |
| PATCH | `/api/v1/admin/missions/{id}/status?value=ACTIVE\|HIDDEN` | 노출 상태 변경 |

### 프런트엔드 (admin-web)

- `src/api/missions.ts` — 위 5개 호출. 상태변경은 `params: { value }`로 전송(컨트롤러 `@RequestParam("value")`와 일치)
- `src/pages/MissionsPage.tsx` — 목록 테이블 + 생성/수정 모달 + 상태 토글
- `src/App.tsx` (수정) — `/missions` 라우트 추가
- `src/constants/menu.ts` (수정) — `AD-16 미션 관리` 메뉴(권한 CONTENT_CREATOR/OPERATOR)

## 3. 스코프 격리 (중요)

복원 직전 작업 트리의 `App.tsx`·`menu.ts`에 **다른 세션의 praise→music 통합 WIP**(dev 미반영)가 섞여 있었다. 이 PR이 그 변경을 끌어들이지 않도록:

1. 두 파일을 `git checkout dev --` 로 dev 상태로 되돌림(찬양·배경음악 공존 상태 = dev 그대로).
2. 미션 라우트/메뉴 항목만 다시 추가.
3. `git diff dev -- App.tsx menu.ts` 로 **미션 추가분만** 있음을 확인.

즉 이 PR의 admin-web 변경은 미션 화면 등록뿐이며, praise/music 관련 변경은 포함하지 않는다.

## 4. 검증

- `./gradlew :admin-server:compileJava` → BUILD SUCCESSFUL
- `./gradlew :admin-server:test` (전체, ArchUnit/Modulith 경계 포함) → BUILD SUCCESSFUL
- admin-web `tsc --noEmit` → 0 errors
- admin-web `npm run build` → 성공(약 4초)
- `git diff dev` 로 App.tsx/menu.ts에 미션 외 변경 없음 확인

도메인 경계: `domain.mission`은 admin 도메인의 `api/VerifyAdminRoleUseCase`만 의존(인가). 다른 도메인의 internal 직접 import 없음 → 경계 테스트 통과.

## 5. 남은 stash 복원 (순서)

1. 미션 관리(AD-16) — 본 PR ✅
2. 앱 버전/업데이트 관리(AD-19) — Flyway 마이그레이션을 다음 빈 번호(V50)로 재번호화 필요
3. 셀프테스트(AD-18)
4. 나눔 공유글 관리(AD-15) — 결합도 가장 높음(AdminSharingService 다수 UseCase), 마지막

## 6. 참고

- 미션 정의/진행 분리: `MissionDefinition`(카탈로그) ↔ `MemberMissionProgress`(회원별 진행). 본 작업은 정의만.
- AD-13은 회원 관리가 선점 → 미션은 AD-16 사용.
