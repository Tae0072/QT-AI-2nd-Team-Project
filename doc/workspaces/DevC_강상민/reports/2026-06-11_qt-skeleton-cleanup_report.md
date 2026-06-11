# QT 구형 스켈레톤 제거 리포트 - 2026-06-11

## 요약

- PR Size Check를 통과하도록 1차 PR 범위를 `service-bible` 중심으로 축소했다.
- 현재 Today QT 흐름과 맞지 않는 `service-bible` 구형 TODO 스켈레톤 계약을 제거했다.
- 실제 사용자 API 계약인 `GetTodayQtUseCase`, `TodayQtResponse`와 저장소 구현인 `QtPassageRepository` 계열은 유지했다.
- 삭제 타입명이 주석 검색에 남지 않도록 `service-note`의 legacy mock 언급도 일반 표현으로 정리했다.
- OpenAPI, DB 마이그레이션, seed 데이터는 변경하지 않았다.

## 변경 파일

### 삭제

- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtRepository.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/client/ai/GenerateAiResponseUseCaseMock.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/client/member/GetMemberUseCaseMock.java`

### 수정

- `qtai-server/service-note/src/main/java/com/qtai/domain/sharing/client/member/MemberRestClientAdapter.java`
  - 삭제된 `GetMemberUseCaseMock` 이름이 Javadoc 검색에 남지 않도록 일반 표현으로 정리

### 생성

- `doc/workspaces/DevC_강상민/workflows/2026-06-11_qt-skeleton-cleanup.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-11_qt-skeleton-cleanup_report.md`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `rg -n "\b(GetQtUseCase\|QtResponse\|QtRepository\|GenerateAiResponseUseCaseMock\|GetMemberUseCaseMock)\b" qtai-server/service-bible qtai-server/service-note` | 결과 없음(exit 1), 1차 PR 삭제 대상 타입 잔여 참조 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :service-note:compileJava` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :service-note:test` | 성공 |

## 생략한 검증

- `.\qtai-server\gradlew.bat -p qtai-server build`: 이번 변경은 `service-bible` 구형 스켈레톤 삭제와 `service-note` 주석 정리로 한정되어 관련 2개 모듈 컴파일/테스트로 검증 범위를 제한했다.
- `gitleaks detect --source . --redact --exit-code 1`: 코드/문서 삭제와 workflow/report 추가, 주석 정리만 포함되어 secret 추가 경로가 없으므로 실행하지 않았다.
- Spectral OpenAPI lint: OpenAPI를 변경하지 않아 실행하지 않았다.

## 수용 기준 확인

- [x] `service-bible` QT 구형 TODO 스켈레톤 파일 제거
- [x] 현재 Today QT 계약과 구현 미변경
- [x] 1차 PR 삭제 대상 타입 이름의 잔여 참조 없음
- [x] PR 변경 파일 수 8 files로 축소
- [x] OpenAPI와 DB 마이그레이션 미변경

## 편차

- 최초 계획은 `admin-server`, `service-ai` 복사본까지 한 PR에서 제거하는 것이었으나, PR Size Check 실패로 1차 PR 범위를 `service-bible` 중심으로 축소했다.
- `admin-server`, `service-ai`의 동일 구형 QT 스켈레톤 제거는 후속 PR로 분리한다.

## 후속 작업

- `admin-server`, `service-ai`의 `GetQtUseCase`, `QtResponse`, `QtRepository`, TODO mock 잔재 제거를 별도 PR로 진행한다.
