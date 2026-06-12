# 성경 검색/장 목록 TODO 계약 후속 제거 리포트 - 2026-06-11

## 요약

- 1차 PR에서 PR Size Check를 고려해 제외한 `service-ai`의 성경 검색/장 목록 TODO 계약 복사본을 제거했다.
- 실제 사용 중인 `GetBibleVerseUseCase`, `ListBibleBooksUseCase`와 `GetBibleVerseRestClientAdapter` 흐름은 변경하지 않았다.
- OpenAPI, DB 마이그레이션, 기존 `/api/v1/bible/**` REST 호출은 변경하지 않았다.

## 변경 파일

### 삭제

- `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/SearchBibleUseCase.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/ListChaptersUseCase.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/bible/api/dto/BibleSearchRequest.java`

### 생성

- `doc/workspaces/DevC_강상민/workflows/2026-06-11_bible-api-skeleton-cleanup-followup.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-11_bible-api-skeleton-cleanup-followup_report.md`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `rg -n "SearchBibleUseCase\|ListChaptersUseCase\|BibleSearchRequest" qtai-server/service-ai` | 결과 없음(exit 1), 삭제 대상 타입 잔여 참조 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-ai:test` | 성공 |

## 생략한 검증

- `.\qtai-server\gradlew.bat -p qtai-server build`: 변경 범위가 `service-ai`의 미사용 TODO 계약 삭제와 문서 추가로 한정되어 관련 모듈 컴파일/테스트로 검증 범위를 제한했다.
- `gitleaks detect --source . --redact --exit-code 1`: 코드 삭제와 workflow/report 추가만 포함되어 secret 추가 경로가 없으므로 실행하지 않았다.
- Spectral OpenAPI lint: OpenAPI를 변경하지 않아 실행하지 않았다.

## 수용 기준 확인

- [x] `service-ai`의 대상 TODO 스켈레톤 3개 제거
- [x] 삭제 대상 타입명의 Java 참조 없음
- [x] `GetBibleVerseUseCase`, `ListBibleBooksUseCase`와 기존 Bible REST 호출 흐름 미변경
- [x] OpenAPI와 DB 마이그레이션 미변경

## 후속 작업

- 없음
