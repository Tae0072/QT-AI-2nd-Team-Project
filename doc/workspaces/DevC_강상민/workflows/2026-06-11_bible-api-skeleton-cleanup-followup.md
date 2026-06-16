# Workflow - 2026-06-11 bible-api-skeleton-cleanup-followup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/bible-api-skeleton-cleanup-followup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | 1차 성경 검색/장 목록 TODO 계약 제거 PR에서 PR Size Check를 고려해 분리한 `service-ai` 복사본 정리 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md`, `doc/workspaces/DevA_이지윤/workflows/2026-05-26_bible-read-api-spec.md` |
| 해당 경로 | `qtai-server/service-ai/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`service-ai`에 남은 `SearchBibleUseCase`, `ListChaptersUseCase`, `BibleSearchRequest` 복사본은 실제 import, 구현체, 주입, 호출 없이 TODO 주석만 남은 스켈레톤이다. 1차 PR과 동일한 판단에 따라 새 구현 없이 제거한다.

## 범위

- `service-ai`의 성경 검색/장 목록 TODO 계약 3개 제거
- 삭제 대상 타입명 잔여 참조 확인
- `service-ai` 컴파일/테스트 검증
- 변경 내용과 검증 결과 report 기록

## 제외 범위

- `service-bible`, `admin-server` 변경
- `GetBibleVerseUseCase`, `ListBibleBooksUseCase` 변경
- `GetBibleVerseRestClientAdapter`와 기존 Bible REST 호출 흐름 변경
- 성경 검색/키워드 검색/FULLTEXT API 구현
- OpenAPI, DB 마이그레이션 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/SearchBibleUseCase.java` | 미사용 검색 UseCase 복사본 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/ListChaptersUseCase.java` | 미사용 장 목록 UseCase 복사본 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/dto/BibleSearchRequest.java` | 미사용 검색 요청 DTO 복사본 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_bible-api-skeleton-cleanup-followup_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `dev` 기준 `chore/bible-api-skeleton-cleanup-followup` 브랜치를 만든다.
2. 이 workflow 문서를 저장한다.
3. `service-ai`의 삭제 대상 파일 3개를 제거한다.
4. `rg`로 삭제 타입명 잔여 참조를 확인한다.
5. `service-ai` 컴파일과 테스트를 실행한다.
6. report에 변경 내용, 검증 결과, 생략한 검증 사유를 기록한다.
7. `refactor(bible): 미사용 성경 검색 스켈레톤 후속 제거` 메시지로 커밋한다.

## 테스트 검증 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | 공백 오류 확인 |
| `rg -n "SearchBibleUseCase\|ListChaptersUseCase\|BibleSearchRequest" qtai-server/service-ai` | 삭제 대상 타입 잔여 참조 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava` | service-ai 컴파일 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-ai:test` | service-ai 테스트 확인 |

## 수용 기준

- [ ] `service-ai`의 대상 TODO 스켈레톤 3개가 제거된다.
- [ ] 삭제 대상 타입명의 Java 참조가 남지 않는다.
- [ ] `GetBibleVerseUseCase`, `ListBibleBooksUseCase`와 기존 Bible REST 호출 흐름은 변경하지 않는다.
- [ ] OpenAPI와 DB 마이그레이션은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 `service-ai`의 삭제 대상 3개로 작다.
- 삭제 대상과 유지 대상이 같은 bible API 패키지에 있어 직접 확인이 안전하다.
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
rg -n "SearchBibleUseCase|ListChaptersUseCase|BibleSearchRequest" qtai-server/service-ai
.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava
.\qtai-server\gradlew.bat -p qtai-server :service-ai:test
```

## 후속 작업으로 남길 항목

- 없음
