# 2026-05-27 note-meditation-lifecycle 구현 리포트

## 기준

- Workflow: `doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-meditation-lifecycle.md`
- 브랜치: `feature/note-meditation-lifecycle`
- 관련 F-ID: F-03, F-13
- 실행 방식: 직접 실행

루트 워크스페이스가 반복적으로 다른 브랜치로 자동 전환되어 변경이 사라지는 현상이 있어, 보존 가능한 sibling worktree
`C:\Workspace\QT-AI-2nd-Team-Project-note-meditation-lifecycle`에서 같은 브랜치 기준으로 구현과 검증을 수행했다.

## 변경 내용

- `Note`의 `activeUniqueKey` 갱신을 상태 전이 메서드로 모아 `MEDITATION`의 `DRAFT`와 `SAVED`는 `ACTIVE`, `DELETED`는 `NULL`이 되도록 고정했다.
- `NoteService` 입력 정규화에서 `MEDITATION`은 `title`, `body`, 4개 섹션 중 하나라도 의미 있는 값이 있으면 저장 가능하도록 보정했다.
- `NoteServiceTest`에 `MEDITATION` `DRAFT`/`SAVED` 생성, 섹션 단독 저장, 읽기 불가 QT, `DELETED` 요청, 활성 중복 수정, 없는 구절 저장 중단, 삭제 시 `savedAt` 정리 검증을 추가했다.
- `NoteControllerTest`에 `GET draft`, `GET detail`, `POST`, `PATCH`, `DELETE`의 인증 주체 null 가드를 추가했다.
- `NoteRepositoryIntegrationTest`에 `findDraft`가 `DRAFT`만 반환하는지, 활성 `MEDITATION` 중복 제약이 동작하는지, 삭제 후 재작성 가능한지 검증을 추가했다.
- `JpaEntityDdlTest`에 `active_unique_key`, `saved_at`, `deleted_at` nullable 컬럼과 활성 묵상 unique 제약 메타데이터 검증을 추가했다.

## 수용 기준 점검

- `MEDITATION` `DRAFT` 생성: `activeUniqueKey=ACTIVE`, `visibility=PRIVATE`, `savedAt=NULL` 테스트로 확인.
- `MEDITATION` `SAVED` 생성: `activeUniqueKey=ACTIVE`, `savedAt` 기록 테스트로 확인.
- `SAVED -> DRAFT`: 기존 테스트로 `savedAt=NULL` 확인.
- 삭제: `status=DELETED`, `deletedAt` 기록, `savedAt=NULL`, `activeUniqueKey=NULL` 테스트로 확인.
- 삭제 후 재작성: Repository 통합 테스트로 확인.
- 활성 중복 차단: Service 단위 테스트와 Repository 통합 테스트로 확인.
- 삭제 노트 수정 차단, 본인 삭제 노트 재삭제 멱등 성공: 기존 및 보강 테스트로 확인.
- 섹션 단독 저장: `title/body` 없이 `interpretSection`만 있는 `MEDITATION` 저장 성공 테스트로 확인.
- `verseIds` 중복 제거와 첫 등장 순서 저장: 기존 테스트 유지, 없는 절 포함 시 저장 중단 테스트 추가.
- Controller Repository 직접 호출 없음: 구조상 UseCase 위임만 유지.
- note 도메인 금지 import: `rg` 검증에서 매칭 없음.

## 검증 결과

- `git diff --check`: 성공
- `.\gradlew.bat test --tests "*Note*" --tests "*JpaEntityDdlTest" --tests "*ArchitectureBoundaryTest"`: 성공
- `.\gradlew.bat build`: 성공
- `rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"`: 매칭 없음
- 금지 콘텐츠 검색: 변경 파일에는 신규 매칭 없음. 기존 설정 파일의 `spring.datasource.password` 키와 workflow 문서의 점검 문구만 매칭됨.

## 실행 불가 또는 실패한 검증

- `.\gradlew.bat test jacocoTestReport`: `jacocoTestReport` task가 현재 `qtai-server` Gradle 프로젝트에 없음.
- `.\gradlew.bat jacocoTestCoverageVerification`: `jacocoTestCoverageVerification` task가 현재 `qtai-server` Gradle 프로젝트에 없음.
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`: 저장소 루트에 `.spectral.yaml`이 없어 실행 실패.
- `gitleaks detect --source . --redact --exit-code 1`: 로컬에 `gitleaks` 명령이 설치되어 있지 않아 실행 실패.

## Lead 검토 필요

- `05_시퀀스_다이어그램.md`의 `active_unique_key` 설명이 상위 문서의 `DRAFT`/`SAVED` 모두 활성 기준과 다른 표현을 포함하므로 문서 정합화가 필요하다.
- `04_API_명세서.md`의 `MEDITATION verseIds` 범위 제한 문구는 `@`멘션 자유 인용 정책과 충돌 가능성이 있어 재확정이 필요하다.

## 후속 작업

- `GET /api/v1/me/meditation-calendar` 본문 구현 시 `status=SAVED`, `deletedAt IS NULL`, `savedAt` 기준 집계를 적용한다.
- 공유 스냅샷 생성과 원본 삭제 시각 반영은 `domain.sharing` 소유 workflow에서 처리한다.
