# Workflow - 2026-06-16 admin-batch-toggle-defaults

| 항목 | 내용 |
| --- | --- |
| 담당자 | DevC 강상민 |
| 작업 브랜치 | `bugfix/admin-batch-toggle-defaults` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 - 관리자 서버 배치 중복 실행 방지 |
| 트리거 | SU 오늘 QT 수집과 AI 해설 시딩 스케줄러가 admin-server 복사본에서도 기본 실행될 수 있는 위험을 제거 |
| 기준 문서 | `AGENTS.md`, `doc/프로젝트 문서/03_아키텍처_정의서.md`, `doc/프로젝트 문서/18_코드_품질_게이트.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`admin-server`에 복사되어 있는 00:02 SU 오늘 QT 수집과 00:05 AI 해설 시딩 스케줄러가 설정 누락 시 자동 실행되지 않도록 기본값을 비활성화한다. 실제 일일 배치 소유권은 기존처럼 `service-bible`과 `service-ai`에 남기고, 소유 단일화 구조 개편은 Lead 결정 이후 별도 작업으로 둔다.

## 범위

- `admin-server`의 `qt.today-source.sum.enabled` yml 기본값이 `false`인지 확인하고 유지한다.
- `admin-server`의 `ai.daily-qt-verse-seed.enabled` yml 기본값이 `false`인지 확인하고 유지한다.
- 두 scheduler 생성자 `@Value` fallback을 `false`로 맞춰 yml 누락 시에도 비활성화되게 한다.
- 기존 toggle 테스트와 빌드 검증으로 회귀를 확인한다.

## 제외 범위

- `service-bible`의 SU 오늘 QT 수집 기본 동작은 변경하지 않는다.
- `service-ai`의 AI 해설 시딩 기본 동작은 변경하지 않는다.
- scheduler 클래스 제거, 모듈 이동, 배치 소유권 재설계는 하지 않는다.
- DB migration, API, DTO, 관리자 UI는 변경하지 않는다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/SuTodayPassageImportScheduler.java` | SU 수집 scheduler fallback 기본값 비활성화 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiDailyQtVerseExplanationSeedScheduler.java` | AI 해설 시딩 scheduler fallback 기본값 비활성화 |
| Verify | `qtai-server/admin-server/src/main/resources/application.yml` | yml 기본값 false 유지 확인 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-16_admin-batch-toggle-defaults_report.md` | 작업 결과와 검증 기록 |

## 구현 순서

1. `origin/dev` 기준 `bugfix/admin-batch-toggle-defaults` 브랜치를 생성한다.
2. `admin-server/src/main/resources/application.yml`에서 두 토글 기본값이 `false`인지 확인한다.
3. `SuTodayPassageImportScheduler`의 `@Value("${qt.today-source.sum.enabled:true}")`를 `false` fallback으로 변경한다.
4. `AiDailyQtVerseExplanationSeedScheduler`의 `@Value("${ai.daily-qt-verse-seed.enabled:true}")`를 `false` fallback으로 변경한다.
5. admin-server 안에 `enabled:true` fallback이 남지 않았는지 검색한다.
6. 지정 테스트와 bootJar, `git diff --check`를 실행한다.
7. report를 작성하고 Conventional Commits 형식으로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 |
| --- | --- |
| `SuTodayPassageImportSchedulerToggleTest` | scheduler disabled 상태에서 startup/정기 수집이 외부 client와 repository를 호출하지 않음 |
| `AiDailyQtVerseExplanationSeedSchedulerToggleTest` | scheduler disabled 상태에서 seed service와 monitoring service를 호출하지 않음 |

## 수용 기준

- [ ] `admin-server` yml의 두 배치 토글 기본값이 `false`다.
- [ ] `admin-server` scheduler 코드에 `qt.today-source.sum.enabled:true` fallback이 남지 않는다.
- [ ] `admin-server` scheduler 코드에 `ai.daily-qt-verse-seed.enabled:true` fallback이 남지 않는다.
- [ ] `service-bible`, `service-ai` scheduler 기본 동작은 변경되지 않는다.
- [ ] 지정 테스트와 `bootJar`, `git diff --check`가 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `admin-server` scheduler fallback 두 곳과 문서에 집중되어 있다.
- 병렬 작업으로 나눌 만큼 독립적인 구현 단위가 없고, 테스트도 같은 변경 의도를 확인한다.
- 메인 에이전트가 직접 실행하는 편이 범위 이탈과 불필요한 리팩터링을 줄인다.

### 위임 가능한 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 검증, report 작성, 커밋을 직접 수행한다.

## 검증 계획

```powershell
.\gradlew :admin-server:test --tests com.qtai.domain.qt.internal.SuTodayPassageImportSchedulerToggleTest
.\gradlew :admin-server:test --tests com.qtai.domain.ai.internal.AiDailyQtVerseExplanationSeedSchedulerToggleTest
.\gradlew :admin-server:bootJar
git diff --check
rg -n "qt\.today-source\.sum\.enabled:true|ai\.daily-qt-verse-seed\.enabled:true" qtai-server/admin-server
git diff -- qtai-server/service-bible qtai-server/service-ai
```

## 후속 작업으로 남길 항목

- 배치 scheduler의 최종 단일 소유 구조와 admin-server 복사본 제거 여부는 Lead 결정 후 별도 작업으로 진행한다.
