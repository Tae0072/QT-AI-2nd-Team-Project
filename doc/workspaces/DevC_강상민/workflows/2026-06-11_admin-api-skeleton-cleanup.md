# Workflow - 2026-06-11 admin-api-skeleton-cleanup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/admin-api-skeleton-cleanup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 관리자 공통 API 영역에 실제 구현 없이 TODO만 남은 스켈레톤 정리 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/admin-server/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`GetStatsUseCase`, `LookupMemberUseCase`, `ModerateContentUseCase`는 관리자 공통 도메인에 남아 있는 TODO 기반 스켈레톤이다. 현재 관리자 대시보드는 `GetAdminDashboardUseCase`와 `/api/v1/admin/dashboard`로 구현되어 있고, 회원/신고/콘텐츠 처리는 각 도메인의 관리자 API가 담당하므로 공통 관리자 UseCase로 재정의하지 않고 제거한다.

## 범위

- `admin-server`의 미사용 관리자 공통 API 스켈레톤 3개 제거
- `AdminService`의 후속 구현 예정 TODO 주석 정리
- 삭제 대상 타입명 잔여 참조 확인
- `admin-server` 컴파일과 테스트 검증
- 변경 내용과 검증 결과 report 기록

## 제외 범위

- `GetAdminDashboardUseCase`, `AdminDashboardService`, `/api/v1/admin/dashboard` 변경
- `member`, `report`, `ai`, `qt`, `notification` 도메인의 실제 관리자 API 변경
- OpenAPI, DB 마이그레이션, 권한 정책 변경
- 전체 관리자 기능 재설계

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/api/GetStatsUseCase.java` | 미사용 통계 조회 스켈레톤 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/api/LookupMemberUseCase.java` | 미사용 회원 조회 스켈레톤 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/api/ModerateContentUseCase.java` | 미사용 콘텐츠 조치 스켈레톤 제거 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/admin/internal/AdminService.java` | 삭제 대상 UseCase 후속 구현 주석 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_admin-api-skeleton-cleanup_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `dev` 기준 `chore/admin-api-skeleton-cleanup` 브랜치를 만든다.
2. 이 workflow 문서를 저장한다.
3. 삭제 대상 스켈레톤 인터페이스 3개를 제거한다.
4. `AdminService`의 삭제 대상 후속 구현 TODO 주석을 제거한다.
5. `rg`로 삭제 타입명 잔여 참조를 확인한다.
6. `admin-server` 컴파일과 테스트를 실행한다.
7. report에 변경 내용, 검증 결과, 생략한 검증 사유를 기록한다.
8. `refactor(admin): 미사용 관리자 공통 API 스켈레톤 제거` 메시지로 커밋한다.

## 테스트 검증 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | 공백 오류 확인 |
| `rg -n "GetStatsUseCase\|LookupMemberUseCase\|ModerateContentUseCase" qtai-server/admin-server` | 삭제 대상 타입 잔여 참조 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava` | 관리자 서버 컴파일 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test` | 관리자 서버 테스트 확인 |

## 수용 기준

- [ ] TODO 기반 관리자 공통 API 스켈레톤 3개가 제거된다.
- [ ] `AdminService`에 삭제 대상 후속 구현 주석이 남지 않는다.
- [ ] 삭제 대상 타입명의 Java 참조가 남지 않는다.
- [ ] 실제 관리자 대시보드와 도메인별 관리자 API는 변경하지 않는다.
- [ ] OpenAPI와 DB 마이그레이션은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 삭제 중심이고 파일 수가 작다.
- 삭제 대상과 주석 정리가 같은 관리자 도메인 맥락에 있어 직접 확인이 안전하다.
- PR Size Check를 고려해 변경 파일 수를 직접 통제해야 한다.

### 위임 가능 작업

| Worker | 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 직접 실행 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

```powershell
git diff --check
rg -n "GetStatsUseCase|LookupMemberUseCase|ModerateContentUseCase" qtai-server/admin-server
.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test
```

## 후속 작업으로 남길 항목

- 없음
