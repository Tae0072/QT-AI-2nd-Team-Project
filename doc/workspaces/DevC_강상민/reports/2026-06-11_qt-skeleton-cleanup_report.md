# QT 구형 스켈레톤 제거 리포트 - 2026-06-11

## 요약

- 현재 Today QT 흐름과 맞지 않는 구형 TODO 스켈레톤 계약을 제거했다.
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
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtRepository.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/client/ai/GenerateAiResponseUseCaseMock.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/client/member/GetMemberUseCaseMock.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/qt/api/GetQtUseCase.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/qt/api/dto/QtResponse.java`

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
| `rg -n "\b(GetQtUseCase\|QtResponse\|QtRepository\|GenerateAiResponseUseCaseMock\|GetMemberUseCaseMock)\b" qtai-server` | 결과 없음(exit 1), 삭제 대상 타입 잔여 참조 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :admin-server:compileJava :service-ai:compileJava :service-note:compileJava` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :admin-server:test :service-ai:test :service-note:test` | 성공 |

## 생략한 검증

- `.\qtai-server\gradlew.bat -p qtai-server build`: 이번 변경은 구형 스켈레톤 삭제와 주석 정리로 한정되어 관련 4개 모듈 컴파일/테스트로 검증 범위를 제한했다.
- `gitleaks detect --source . --redact --exit-code 1`: 코드/문서 삭제와 workflow/report 추가, 주석 정리만 포함되어 secret 추가 경로가 없으므로 실행하지 않았다.
- Spectral OpenAPI lint: OpenAPI를 변경하지 않아 실행하지 않았다.

## 수용 기준 확인

- [x] QT 구형 TODO 스켈레톤 파일 제거
- [x] 현재 Today QT 계약과 구현 미변경
- [x] 삭제된 타입 이름의 Java 참조 없음
- [x] 관련 모듈 컴파일과 테스트 결과 기록
- [x] OpenAPI와 DB 마이그레이션 미변경

## 편차

- 계획의 주 검증 대상은 `service-bible`, `admin-server`, `service-ai`였으나, `service-note` Javadoc에 삭제된 mock 이름이 남아 있어 주석을 함께 정리했다.
- 이에 따라 `service-note:compileJava`와 `service-note:test`도 추가 실행했다.

## 후속 작업

- 독립 QT CRUD 또는 AI 피드백형 QT 기능이 다시 필요하면 기준 문서, F-ID, API 계약을 먼저 확정한 뒤 별도 작업으로 진행한다.
