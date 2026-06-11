# 성경 검색/장 목록 TODO 계약 제거 리포트 - 2026-06-11

## 요약

- 실제 사용처가 없는 성경 검색/장 목록 TODO 계약을 `service-bible`, `admin-server`에서 제거했다.
- 현재 공식 Bible API와 실제 Java 계약인 `ListBibleBooksUseCase`, `GetBibleVerseUseCase`는 변경하지 않았다.
- `service-ai`의 동일 복사본은 PR Size Check를 고려해 후속 PR로 분리한다.
- OpenAPI, DB 마이그레이션, 기존 `/api/v1/bible/**` 조회 API는 변경하지 않았다.

## 변경 파일

### 삭제

- `qtai-server/service-bible/src/main/java/com/qtai/domain/bible/api/SearchBibleUseCase.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/bible/api/ListChaptersUseCase.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/bible/api/dto/BibleSearchRequest.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/bible/api/SearchBibleUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/bible/api/ListChaptersUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/bible/api/dto/BibleSearchRequest.java`

### 생성

- `doc/workspaces/DevC_강상민/workflows/2026-06-11_bible-api-skeleton-cleanup.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-11_bible-api-skeleton-cleanup_report.md`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `rg -n "SearchBibleUseCase\|ListChaptersUseCase\|BibleSearchRequest" qtai-server/service-bible qtai-server/admin-server` | 결과 없음(exit 1), 1차 범위 잔여 참조 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :admin-server:compileJava` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :admin-server:test` | 성공 |

## 생략한 검증

- `.\qtai-server\gradlew.bat -p qtai-server build`: 변경 범위가 `service-bible`, `admin-server`의 미사용 TODO 계약 삭제와 문서 추가로 한정되어 관련 모듈 컴파일/테스트로 검증 범위를 제한했다.
- `gitleaks detect --source . --redact --exit-code 1`: 코드 삭제와 workflow/report 추가만 포함되어 secret 추가 경로가 없으므로 실행하지 않았다.
- Spectral OpenAPI lint: OpenAPI를 변경하지 않아 실행하지 않았다.

## 수용 기준 확인

- [x] `service-bible`, `admin-server`의 대상 TODO 스켈레톤 6개 제거
- [x] 삭제 대상 타입명의 1차 범위 Java 참조 없음
- [x] `ListBibleBooksUseCase`, `GetBibleVerseUseCase`와 기존 Bible API 미변경
- [x] OpenAPI와 DB 마이그레이션 미변경
- [x] `service-ai` 복사본은 후속 PR로 분리

## 후속 작업

- `chore/bible-api-skeleton-cleanup-followup`에서 `service-ai`의 `SearchBibleUseCase`, `ListChaptersUseCase`, `BibleSearchRequest` 복사본 제거
