# Workflow - 2026-06-11 qt-skeleton-cleanup-followup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/qt-skeleton-cleanup-followup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 1차 QT 스켈레톤 정리 PR에서 PR Size Check 대응으로 제외한 admin-server/service-ai 잔재 정리 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/admin-server/**`, `qtai-server/service-ai/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

현재 QT 흐름은 `GetTodayQtUseCase`, `TodayQtResponse`, `QtPassageRepository`, `QtPassageLookup`을 사용한다. 후속 PR에서는 `admin-server`, `service-ai`에 남은 과거 개인 QT CRUD/AI 피드백 구조 기반 TODO 스켈레톤만 제거한다.

## 범위

- `admin-server`의 `GetQtUseCase`, `QtResponse`, `QtRepository`, `GenerateAiResponseUseCaseMock`, `GetMemberUseCaseMock` 삭제
- `service-ai`의 `GetQtUseCase`, `QtResponse` 삭제
- 삭제 대상 타입명 잔여 참조 확인
- 관련 모듈 컴파일/테스트 확인

## 제외 범위

- `service-bible` 변경
- `GetTodayQtUseCase`, `TodayQtResponse`, `GetQtPassageContentContextUseCase` 변경
- `QtPassageRepository`, `QtPassageLookup`, `QtService`, 관리자 QT 실제 구현 변경
- OpenAPI, DB 마이그레이션, seed 데이터 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java` | 미사용 구형 QT 조회 포트 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java` | 미사용 구형 QT 응답 DTO 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtRepository.java` | 미사용 빈 저장소 스켈레톤 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/client/**UseCaseMock.java` | 등록되지 않은 TODO mock 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java` | 미사용 구형 QT 조회 포트 복사본 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java` | 미사용 구형 QT 응답 DTO 복사본 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_qt-skeleton-cleanup-followup_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `dev` 기준 `chore/qt-skeleton-cleanup-followup` 브랜치를 만든다.
2. 이 workflow 문서를 저장한다.
3. 삭제 대상 파일만 제거한다.
4. `rg`로 삭제된 타입 이름의 잔여 참조를 확인한다.
5. `admin-server`, `service-ai` 컴파일과 테스트를 실행한다.
6. report에 변경 내용과 검증 결과를 기록한다.
7. `refactor(qt): 미사용 QT 스켈레톤 후속 제거` 메시지로 커밋한다.

## 테스트 검증 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | 공백 오류 확인 |
| `rg -n "\b(GetQtUseCase\|QtResponse\|QtRepository\|GenerateAiResponseUseCaseMock\|GetMemberUseCaseMock)\b" qtai-server/admin-server/src/main/java qtai-server/service-ai/src/main/java` | 삭제 대상 타입 잔여 참조 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava :service-ai:compileJava` | 관련 모듈 컴파일 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test :service-ai:test` | 관련 모듈 테스트 확인 |

## 수용 기준

- [ ] `admin-server`, `service-ai` QT 구형 TODO 스켈레톤 파일이 제거된다.
- [ ] 현재 Today QT 계약과 관리자 QT 실제 구현은 변경되지 않는다.
- [ ] 삭제된 타입 이름의 Java 참조가 남지 않는다.
- [ ] PR 변경 파일 수가 10 files 이하로 유지된다.
- [ ] OpenAPI와 DB 마이그레이션은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 삭제 중심이고 파일 수가 작다.
- 삭제 대상과 유지 대상이 같은 QT 패키지에 섞여 있어 직접 실행이 안전하다.
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
rg -n "\b(GetQtUseCase|QtResponse|QtRepository|GenerateAiResponseUseCaseMock|GetMemberUseCaseMock)\b" qtai-server/admin-server/src/main/java qtai-server/service-ai/src/main/java
.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava :service-ai:compileJava
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test :service-ai:test
```

## 후속 작업으로 남길 항목

- 없음
