# Study 도메인 스켈레톤 제거 리포트 - 2026-06-11

## 요약

- 기준 문서에 없는 독립 `/api/v1/study` 목록/상세 TODO 스켈레톤을 제거했다.
- QT 학습 콘텐츠, 해설, 용어, 시뮬레이터 관련 실제 구현과 내부 게시 API는 유지했다.
- OpenAPI, DB 마이그레이션, seed 데이터는 변경하지 않았다.

## 변경 파일

### 삭제

- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/web/StudyController.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/internal/StudyService.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/internal/StudyRepository.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/internal/Study.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/api/GetStudyUseCase.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/api/ListStudyUseCase.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/study/api/dto/StudyResponse.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/internal/StudyService.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/internal/StudyRepository.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/internal/Study.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/api/GetStudyUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/api/ListStudyUseCase.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/study/api/dto/StudyResponse.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/study/api/GetStudyUseCase.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/study/api/ListStudyUseCase.java`
- `qtai-server/service-ai/src/main/java/com/qtai/domain/study/api/dto/StudyResponse.java`

### 생성

- `doc/workspaces/DevC_강상민/workflows/2026-06-11_study-skeleton-cleanup.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-11_study-skeleton-cleanup_report.md`

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | 성공 |
| `rg -n "GetStudyUseCase\|ListStudyUseCase\|StudyResponse\|StudyController\|StudyService\|StudyRepository\|class Study\b" qtai-server` | 결과 없음(exit 1), 삭제 대상 타입 잔여 참조 없음 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :admin-server:compileJava :service-ai:compileJava` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :admin-server:test :service-ai:test` | 성공 |

## 생략한 검증

- `.\qtai-server\gradlew.bat -p qtai-server build`: 이번 변경은 삭제 대상 타입 참조와 관련 3개 모듈 컴파일/테스트로 검증 범위를 한정했다.
- `gitleaks detect --source . --redact --exit-code 1`: 코드/문서 삭제와 내부 workflow/report 추가만 포함되어 secret 추가 경로가 없으므로 실행하지 않았다.
- Spectral OpenAPI lint: OpenAPI를 변경하지 않아 실행하지 않았다.

## 수용 기준 확인

- [x] 독립 Study 목록/상세 TODO 스켈레톤 파일 제거
- [x] QT 학습 콘텐츠/해설/용어/시뮬레이터 구현 유지
- [x] 삭제된 타입 이름의 Java 참조 없음
- [x] 관련 모듈 컴파일과 테스트 결과 기록
- [x] OpenAPI와 DB 마이그레이션 미변경

## 후속 작업

- 독립 Study 목록/상세 기능이 다시 필요하면 기준 문서, F-ID, API 계약을 먼저 확정한 뒤 별도 작업으로 진행한다.
