# Workflow - 2026-06-11 study-skeleton-cleanup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `chore/study-skeleton-cleanup` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | Study 도메인에 기준 문서 밖 독립 목록/상세 TODO 스켈레톤이 남아 있어 정리 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md` |
| 해당 경로 | `qtai-server/service-bible/**`, `qtai-server/admin-server/**`, `qtai-server/service-ai/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

독립 `/api/v1/study` 목록/상세 API를 전제로 한 TODO 스켈레톤을 제거한다. 기준 문서에 명시된 Study 책임은 QT 학습 콘텐츠, 해설, 용어, 시뮬레이터 조회 및 내부 게시 API이므로 해당 구현은 유지한다.

## 범위

- `service-bible`의 `StudyController`, `StudyService`, `StudyRepository`, `Study`, `GetStudyUseCase`, `ListStudyUseCase`, `StudyResponse` 삭제
- `admin-server`의 `StudyService`, `StudyRepository`, `Study`, `GetStudyUseCase`, `ListStudyUseCase`, `StudyResponse` 삭제
- `service-ai`의 `GetStudyUseCase`, `ListStudyUseCase`, `StudyResponse` 삭제
- 삭제 후 잔여 참조 검색, 관련 모듈 컴파일/테스트 확인
- 완료 report 작성 및 Conventional Commits 커밋

## 제외 범위

- QT 학습 콘텐츠, 해설, 용어, 시뮬레이터 관련 실제 구현 변경
- `/api/v1/study/verse-explanations`, `/api/v1/study/glossary-terms`, `/api/v1/qt/{qtPassageId}/study-content` 계약 변경
- OpenAPI 변경
- 독립 Study 목록/상세 API 신규 구현
- DB 마이그레이션 또는 seed 데이터 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/study/**/Study*.java` | 미사용 독립 Study 스켈레톤 제거 |
| Delete | `qtai-server/service-bible/src/main/java/com/qtai/domain/study/api/*StudyUseCase.java` | 미사용 목록/상세 포트 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/study/**/Study*.java` | 모놀리식 복사본 잔재 제거 |
| Delete | `qtai-server/admin-server/src/main/java/com/qtai/domain/study/api/*StudyUseCase.java` | 모놀리식 복사본 잔재 제거 |
| Delete | `qtai-server/service-ai/src/main/java/com/qtai/domain/study/api/**/Study*.java` | 미사용 Study 계약 복사본 제거 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-11_study-skeleton-cleanup_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. `chore/study-skeleton-cleanup` 브랜치를 만든다.
2. 이 workflow 문서를 저장한다.
3. 삭제 대상 파일만 제거한다.
4. `rg`로 삭제된 타입 이름의 잔여 참조를 확인한다.
5. 관련 모듈 컴파일과 테스트를 실행한다.
6. report에 변경 내용, 검증 결과, 미실행 명령이 있으면 사유를 기록한다.
7. `refactor(study): 미사용 독립 스터디 스켈레톤 제거` 메시지로 커밋한다.

## 테스트 검증 목록

| 검증 | 내용 |
| --- | --- |
| `git diff --check` | 공백 오류 확인 |
| `rg -n "GetStudyUseCase\|ListStudyUseCase\|StudyResponse\|StudyController\|StudyService\|StudyRepository\|class Study\b" qtai-server` | 삭제 대상 타입 잔여 참조 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :admin-server:compileJava :service-ai:compileJava` | 관련 모듈 컴파일 확인 |
| `.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :admin-server:test :service-ai:test` | 관련 모듈 테스트 확인 |

## 수용 기준

- [ ] 독립 Study 목록/상세 TODO 스켈레톤 파일이 제거된다.
- [ ] QT 학습 콘텐츠/해설/용어/시뮬레이터 구현 파일은 유지된다.
- [ ] 삭제된 타입 이름의 Java 참조가 남지 않는다.
- [ ] 관련 모듈 컴파일과 테스트 결과가 report에 기록된다.
- [ ] OpenAPI와 DB 마이그레이션은 변경하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 삭제 중심이고 삭제 대상 참조 확인이 한 흐름에서 처리되어야 한다.
- 병렬 편집 시 동일 패키지의 실제 Study 기능 파일까지 잘못 삭제할 위험이 커진다.
- 테스트와 report 작성도 삭제 결과를 기준으로 직렬 검증하는 편이 안전하다.

### 위임 가능 작업

| Worker | 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 직접 실행 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 작업을 직접 수행한다.

## 검증 계획

```powershell
git diff --check
rg -n "GetStudyUseCase|ListStudyUseCase|StudyResponse|StudyController|StudyService|StudyRepository|class Study\b" qtai-server
.\qtai-server\gradlew.bat -p qtai-server :service-bible:compileJava :admin-server:compileJava :service-ai:compileJava
.\qtai-server\gradlew.bat -p qtai-server :service-bible:test :admin-server:test :service-ai:test
```

## 후속 작업으로 남길 항목

- 기준 문서에 독립 Study 목록/상세 기능이 새로 추가되면 별도 F-ID와 API 계약을 먼저 확정한다.
