# 리포트 — 관리자 웹 '적용' 버튼 + 업데이트 예정/앱 버전 관리 (AD-19)

- 일자: 2026-06-14
- 작성: Claude (Lead T 요청)
- 관련 워크플로우: `workflows/2026-06-14_admin-app-version-update.md`

## 한 줄 요약
관리자 콘솔에 **전역 '적용' 버튼**(콘텐츠 버전 즉시 게시, 0.1.0→0.1.0.1)과 **'업데이트 예정' 페이지**(앱 재설치 필요 변경을 모아 두었다가 적용 시 앱 출시 버전 업데이트)를 추가했다. 신규 `appversion` 도메인 + 화면.

## 버전 2단계 개념
| 종류 | 예 | 반영 방식 | 올리는 곳 |
|---|---|---|---|
| 콘텐츠 버전 | 0.1.0 → 0.1.0.1 | 앱이 백그라운드로 데이터 갱신(즉시) | 헤더 '적용' 버튼 |
| 앱 출시 버전 | 0.1.0 → 0.2.0 | 앱 재설치/스토어 업데이트 필요 | '업데이트 예정' 항목 '적용' |

- 앱 출시 버전 적용 시 **권장/강제** 안내를 설정하고, 강제면 최소지원버전을 새 버전으로 맞춘다.

## 변경 파일
**백엔드 (qtai-server/admin-server, 신규 domain.appversion)**
| 파일 | 구분 |
|---|---|
| `internal/AppUpdateMode.java`, `internal/PendingUpdateStatus.java` | enum |
| `internal/AppVersionState.java`, `internal/PendingAppUpdate.java` | 엔티티 |
| `internal/AppVersionStateRepository.java`, `internal/PendingAppUpdateRepository.java` | repo |
| `internal/AppVersionService.java` | 서비스 |
| `api/AdminAppVersionUseCase.java` | UseCase |
| `api/dto/AppVersionStateResponse.java`, `PendingAppUpdateResponse.java`, `PendingAppUpdateCreateRequest.java` | DTO |
| `web/AdminAppVersionController.java` | 컨트롤러 |
| `resources/db/migration/V50__create_app_version.sql` | 스키마 (2026-06-15 복원: V44→V50 재번호) |
| `test/.../AppVersionServiceTest.java`, `AdminAppVersionControllerTest.java` | 테스트 |

**프런트 (admin-web)**
| 파일 | 구분 |
|---|---|
| `src/api/appVersion.ts` | API |
| `src/components/layout/AdminLayout.tsx` | 헤더 전역 '적용' 버튼 + 버전 표시 |
| `src/pages/AppUpdatesPage.tsx` | AD-19 페이지(신규) |
| `src/constants/menu.ts`, `src/App.tsx` | 메뉴/라우트 |

## 동작
- **헤더 '적용'**: 어느 화면에서든 보임. 누르면 확인 후 콘텐츠 버전이 한 단계 올라가고 사용자에게 즉시 반영(앱 재설치 불필요). 헤더에 현재 버전(vX) 표시.
- **업데이트 예정 페이지**: 현재 버전 카드 + 콘텐츠 게시 버튼 + 예정 목록(대기/완료/전체). '추가'로 항목 등록(제목·설명·대상 앱 버전·안내 강도), 행별 '적용'으로 앱 출시 버전 업데이트, '삭제' 지원.

## 검증 상태
- 코드 대조 검증(샌드박스 빌드 불가). 신규 테스트 14케이스(서비스 6 + 컨트롤러 8).
- **PR 전 T님 PC 필수**:
  - `./gradlew -p qtai-server :admin-server:build :admin-server:test`
  - `cd admin-web && npm run build && npm run test`
- 권장 수동확인: 헤더 '적용' 후 버전 증가, 예정 추가→적용 시 앱 버전 상승·강제 시 최소버전 동기화, 권한(OPERATOR) 접근.

## 리스크 / 메모
- 사용자 앱(Flutter)의 버전 체크 연동은 **미구현**(후속). 현재는 관리자 측 상태 관리까지. 실제 강제/권장 배너가 동작하려면 앱 실행 시 `/state` 류를 읽는 사용자 API + 라우팅 필요.
- 새 도메인 추가로 CLAUDE.md §3 도메인 목록 변경 → 문서레포 요구사항/아키텍처 반영 필요(Lead 검토).
- 단일행 상태는 Flyway 시드 1행 + 서비스 방어적 생성으로 이중 보장.
