# Workflow - 2026-06-11 qt-skeleton-cleanup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/qt-skeleton-cleanup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | QT 도메인에 현재 Today QT 흐름과 맞지 않는 구형 TODO 스켈레톤 계약이 남아 있어 1차 범위를 작게 정리 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/service-bible/**`, `qtai-server/service-note/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

현재 QT 사용자 흐름은 `GetTodayQtUseCase`, `TodayQtResponse`, `QtPassageRepository`, `QtPassageLookup`, REST client 계열을 사용한다. 1차 PR에서는 PR Size Check를 통과하도록 `service-bible`의 과거 개인 QT CRUD/AI 피드백 구조 기반 TODO 스켈레톤만 제거한다.

## 범위

- `service-bible`의 `GetQtUseCase`, `QtResponse`, `QtRepository`, `GenerateAiResponseUseCaseMock`, `GetMemberUseCaseMock` 삭제
- `service-note` Javadoc에 남은 삭제 mock 클래스명 언급을 일반 표현으로 정리
- 삭제 후 잔여 참조 검색, 관련 모듈 컴파일/테스트 확인
- 완료 report 작성 및 Conventional Commits 커밋

## 제외 범위

- `admin-server`, `service-ai`의 동일 구형 QT 스켈레톤 제거는 후속 PR로 분리
- `GetTodayQtUseCase`, `TodayQtResponse`, `GetQtPassageContentContextUseCase` 변경
- `QtPassageRepository`, `QtPassageLookup`, `QtService`, `QtController` 변경
- OpenAPI, DB 마이그레이션, seed 데이터 변경
- Today QT 응답 구조나 cache/simulator 상태 정책 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java` | 미사용 구형 QT 조회 포트 제거 |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java` | 미사용 구형 QT 응답 DTO 제거 |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtRepository.java` | 미사용 빈 저장소 스켈레톤 제거 |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/client/ai/GenerateAiResponseUseCaseMock.java` | 등록되지 않은 TODO AI mock 제거 |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/client/member/GetMemberUseCaseMock.java` | 등록되지 않은 TODO member mock 제거 |
| Modify | `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/client/member/MemberRestClientAdapter.java` | 삭제 mock 이름이 검색에 남지 않도록 Javadoc 정리 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_qt-skeleton-cleanup_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `dev` 기준 `chore/qt-skeleton-cleanup` 브랜치를 만든다.
2. 이 workflow 문서를 저장한다.
3. `service-bible` 삭제 대상 파일만 제거한다.
4. 삭제 mock 이름이 남은 `service-note` Javadoc을 일반 표현으로 정리한다.
5. `rg`로 삭제된 타입 이름의 잔여 참조를 확인한다.
6. 관련 모듈 컴파일과 테스트를 실행한다.
7. report에 변경 내용, 검증 결과, 후속 PR 항목을 기록한다.
8. `refactor(qt): 미사용 QT 구형 스켈레톤 제거` 메시지로 커밋한다.

## 테스트 검증 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | 공백 오류 확인 |
| `rg -n "\b(GetQtUseCase\|QtResponse\|QtRepository\|GenerateAiResponseUseCaseMock\|GetMemberUseCaseMock)\b" qtai-server/service-bible qtai-server/service-note` | 1차 PR 삭제 대상 타입 잔여 참조 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :service-note:compileJava` | 관련 모듈 컴파일 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :service-note:test` | 관련 모듈 테스트 확인 |

## 수용 기준

- [ ] `service-bible` QT 구형 TODO 스켈레톤 파일이 제거된다.
- [ ] 현재 Today QT 계약과 구현은 변경되지 않는다.
- [ ] 1차 PR 삭제 대상 타입 이름이 `service-bible`, `service-note`에 남지 않는다.
- [ ] PR 변경 파일 수가 10 files 이하로 유지된다.
- [ ] OpenAPI와 DB 마이그레이션은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `service-bible` 삭제와 `service-note` 주석 정리로 작다.
- 삭제 대상과 유지 대상이 같은 `domain.qt` 패키지에 섞여 있어 직접 실행이 안전하다.
- PR Size Check 대응을 위해 파일 수를 통제해야 하므로 병렬 편집 이점이 작다.

### 위임 가능 작업

| Worker | 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 직접 실행 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

```powershell
git diff --check
rg -n "\b(GetQtUseCase|QtResponse|QtRepository|GenerateAiResponseUseCaseMock|GetMemberUseCaseMock)\b" qtai-server/service-bible qtai-server/service-note
.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :service-note:compileJava
.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :service-note:test
```

## 후속 작업으로 남길 항목

- `admin-server`, `service-ai`의 동일 구형 QT 스켈레톤 제거는 별도 PR로 분리한다.
