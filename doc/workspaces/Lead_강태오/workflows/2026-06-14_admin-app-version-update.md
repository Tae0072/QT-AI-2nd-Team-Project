# 2026-06-14 관리자 웹 '적용' 버튼 + 업데이트 예정/앱 버전 관리 (AD-19)

## 요청 (Lead T)
- '적용' 버튼 추가 — 누르면 변경이 반영되도록.
- 왼쪽 네비게이션 모든 화면에서, 백그라운드로 받을 수 있는 가벼운 데이터 변경은 **즉시 반영**하고 **버전을 0.1.0 → 0.1.0.1**로 올린다.
- 데이터가 크거나 **앱 재설치(업데이트)가 필요한 변경**은 즉시 반영 불가 → **'업데이트 예정' 목록 페이지**(왼쪽 네비)에 모아둔다.
- '업데이트 예정'에서 **적용**을 누르면 **앱 출시 버전**이 올라간다. 앱 버전 = 출시 버전 + 권장/강제 업데이트 안내.

## 설계 (2026-06-14 Lead 승인) — 버전 2단계 분리
- **콘텐츠 버전**(예: 0.1.0.1): 앱이 백그라운드로 데이터만 갱신 → 즉시 반영. 헤더 **'적용'** 버튼이 마지막 자리를 +1.
- **앱 출시 버전**(예: 0.1.0): 앱 재설치/스토어 업데이트 필요. '업데이트 예정' 항목 **적용** 시 올라가고, 적용 시 **권장/강제** 안내와 강제면 `minSupported` 동기화.
- 현재 코드에 앱 버전 개념 **없었음** → 신규 `appversion` 도메인 + 화면 신설. (요구사항 변경 → 문서레포 반영은 후속)

## 범위
- admin-server 신규 도메인 `appversion`(admin 고유 기능). 스키마 **admin-server 소유**(V50, 2026-06-15 stash 복원 시 V44→V50 재번호).
- admin-web: API 클라이언트 `api/appVersion.ts`까지 이번 복원 PR에 포함. 헤더 '적용' 버튼·'업데이트 예정' 페이지(AD-19 화면)와 Flutter 버전 체크 연동은 **후속 PR**.

> 복원 메모(2026-06-15): 이 기능은 2026-06-14 작성됐으나 미커밋 stash로만 남아 있었다. dev에 이미 V44~V49가 있어 마이그레이션을 **V50**으로 재번호하고, 백엔드 도메인+테스트+API 클라이언트+문서만 먼저 PR로 복원한다.

## 구현
**백엔드 (admin-server / domain.appversion)**
- 엔티티: `AppVersionState`(단일행: contentVersion·appVersion·minSupportedVersion·updateMode·updateMessage, `bumpContentVersion`/`promoteAppVersion`), `PendingAppUpdate`(title·description·targetAppVersion·updateMode·status·appliedAt).
- enum: `AppUpdateMode(NONE/RECOMMENDED/FORCED)`, `PendingUpdateStatus(PENDING/APPLIED)`.
- repo: `AppVersionStateRepository.findTopByOrderByIdAsc`, `PendingAppUpdateRepository`(상태별/전체/단건, 삭제 제외).
- `AdminAppVersionUseCase` + `AppVersionService`: getState(없으면 기본 생성)·applyContent(콘텐츠 +1)·listPending(status)·createPending·applyPending(앱버전 promote + APPLIED)·deletePending(소프트).
- `AdminAppVersionController` `/api/v1/admin/app-updates`: GET `/state`, POST `/apply-content`, GET `/pending?status=`, POST `/pending`(201), POST `/pending/{id}/apply`, DELETE `/pending/{id}`. 권한 ROLE_ADMIN + admin_users `OPERATOR`(music 컨트롤러 requireRole 패턴 복제).
- Flyway `V50__create_app_version.sql`: `app_version_state`(초기 0.1.0 1행 시드) + `pending_app_updates` + index. H2/MySQL 공용(AUTO_INCREMENT·TIMESTAMP DEFAULT·VARCHAR enum, deleted_at 포함).

**프런트 (admin-web)**
- `api/appVersion.ts`: 상태/적용/목록/등록/적용/삭제 + 타입.
- `components/layout/AdminLayout.tsx`: 헤더에 현재 콘텐츠 버전 태그(클릭 시 /app-updates) + **'적용'** 버튼(Popconfirm → applyContent, 모든 화면 공통).
- `pages/AppUpdatesPage.tsx`(AD-19): 현재 버전 카드 + '콘텐츠 버전 게시' 버튼 + '업데이트 예정' 표(세그먼트 대기/완료/전체, 추가 모달, 행별 **적용**/삭제).
- `constants/menu.ts` AD-19 `/app-updates`(OPERATOR), `App.tsx` 라우트 추가.

## 검증
- ⚠️ 샌드박스 빌드 불가(Windows node_modules·JDK11) → 코드 대조만.
- 신규 테스트:
  - `AppVersionServiceTest`(6): 콘텐츠 +1 / 기본생성 후 +1 / 예정 등록 PENDING / 적용 시 앱버전·강제 minSupported / 이미적용 400 / nextContentVersion 규칙(3자리→패치추가·4자리→증가·자리올림).
  - `AdminAppVersionControllerTest`(8): state·apply-content·create(201)·빈제목 400·apply·delete(204)·401·403.
- 대조: `AdminControllerSurfaceTest`(모두 `/api/v1/admin`) 위반 없음, admin-server엔 모듈경계 테스트 없음(새 도메인 안전), SecurityConfig csrf off·`/api/v1/admin/** hasRole(ADMIN)`.

### PR 전 필수 (T님 PC)
```bash
./gradlew -p qtai-server :admin-server:build :admin-server:test
cd admin-web && npm run build && npm run test
```

## 후속
- 사용자 앱(Flutter) 실행 시 버전 체크 API 연동(권장/강제 배너, minSupported 차단) — service-user + nginx 경로 신설.
- 각 화면 변경을 '업데이트 예정'으로 보내는 바로가기(예: 음원 대량 추가 시) — 선택.
- 문서레포 F-06 범위에 AD-19 반영.

## Git/PR
- 브랜치 제안 `feature/admin-app-version-update` → PR `dev`.
- 커밋: `feat(appversion): 적용 버튼·콘텐츠 버전 게시 + 업데이트 예정/앱 버전 관리(AD-19)`.
