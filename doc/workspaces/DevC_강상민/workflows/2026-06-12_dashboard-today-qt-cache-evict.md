# Workflow - 2026-06-12 dashboard-today-qt-cache-evict

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-01 / AD-02 |
| 트리거 | 오늘 QT 관리에서 게시 후 대시보드는 갱신되지만, 숨김 후 대시보드 오늘 QT 상태가 갱신되지 않음 |
| 기준 문서 | `AGENTS.md`, `admin-web/src/api/dashboard.ts`, `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java`, `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java`, `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/internal/AdminQtPassageServiceTest.java`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자가 오늘 QT를 게시/숨김/수정했을 때 대시보드의 오늘 QT 상태가 즉시 최신 상태를 보도록 한다. 현재 `QtPassageLookup.findTodayPassage()`는 `cacheStatus=HIT`인 오늘 QT 응답만 `todayQt` 캐시에 저장한다. 숨김 처리 후 이 캐시가 비워지지 않아 대시보드가 이전 `READY` 상태를 계속 표시할 수 있다.

## 범위

- 관리자 QT 게시/숨김/수정 성공 시 `todayQt` 캐시를 비운다.
- 캐시 무효화는 관리자 변경 경로에만 적용한다.
- 관련 서비스 테스트 또는 컴파일 검증을 수행한다.
- 브라우저에서 숨김 후 대시보드 상태가 갱신되는지 확인한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 대시보드 프런트 자동 polling 추가
- QT 사용자 노출 시간 정책 변경
- `QtPassageLookup` 캐시 키 구조 변경
- Caffeine/Redis 캐시 구성 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java` | 관리자 QT 변경 후 `todayQt` 캐시 무효화 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/internal/AdminQtPassageServiceTest.java` | 기존 게시/숨김 동작 회귀 확인 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_dashboard-today-qt-cache-evict_report.md` | 원인과 검증 결과 기록 |

## 구현 순서

1. `QtPassageLookup.findTodayPassage()`의 캐시 정책을 확인한다.
2. `AdminQtPassageService.publish`, `hide`, `update`에 `todayQt` 캐시 무효화를 적용한다.
3. 관련 테스트를 실행한다.
4. admin-server를 빌드/재기동한다.
5. 브라우저에서 오늘 QT를 게시/숨김한 뒤 대시보드 새로고침 결과를 확인한다.
6. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AdminQtPassageServiceTest` | 기존 publish/hide 상태 전환 테스트가 통과하는지 확인 |

## 수용 기준

- [ ] 오늘 QT 숨김 후 대시보드가 이전 READY 캐시를 계속 표시하지 않는다.
- [ ] 오늘 QT 게시 후 대시보드가 READY 상태를 표시한다.
- [ ] `admin-server` 관련 테스트가 성공한다.
- [ ] report가 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 원인이 캐시 무효화 한 지점에 집중되어 있다.
- 구현, 테스트, 브라우저 검증이 순차적으로 연결되어 병렬화 이점이 작다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 캐시 정책과 관리자 상태 변경 경로를 직접 확인하고 수정한다.

## 검증 계획

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest"
.\gradlew.bat :admin-server:bootJar
docker compose up -d --build service-admin
```

브라우저 확인:

- `/qt-passages`에서 오늘 QT 게시
- `/dashboard` 새로고침 후 오늘 QT `READY` 확인
- `/qt-passages`에서 같은 QT 숨김
- `/dashboard` 새로고침 후 오늘 QT가 `MISSING` 또는 최신 상태로 변경되는지 확인

## 후속 작업으로 넘길 항목

- 화면 간 이동 시 대시보드 자동 reload/polling UX는 별도 개선으로 분리한다.
