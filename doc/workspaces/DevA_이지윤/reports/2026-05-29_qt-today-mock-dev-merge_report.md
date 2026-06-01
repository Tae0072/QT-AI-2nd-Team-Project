# feature/qt-today-mock dev 병합 충돌 정리 리포트

## 작업 배경

- 대상 브랜치: `feature/qt-today-mock`
- 목적: 최신 `dev` 반영 후 원격 브랜치 push
- 자동 리뷰 BLOCK 대응은 아니며, 최신 `dev` 병합 중 Note 도메인 계약 변경과 충돌이 발생했다.

## 충돌 원인

- 최신 `dev`에서 `NoteSaveResponse`가 삭제되고 `NoteCreateResponse` / `NoteUpdateResponse`로 분리되었다.
- 기존 브랜치에는 구형 노트 응답 DTO와 테스트 기대값 일부가 남아 있어 `NoteService`, `NoteController`, OpenAPI, 테스트 파일에서 충돌이 발생했다.

## 수정 내용

- 병합 충돌은 최신 `dev` 계약을 우선해 해결했다.
- 병합 과정에서 되살아난 `NoteSaveResponse` 파일을 제거했다.
- 최신 `dev` 기준과 맞지 않는 `NoteServiceTest`의 구형 검증 1줄을 제거했다.

## 인접 경로 점검

- `NoteSaveResponse` 참조가 운영 코드, 테스트, OpenAPI에 남아 있지 않은지 확인했다.
- 노트 create/update 전용 에러코드(`NOTE_QT_PASSAGE_REQUIRED`, `NOTE_CONTENT_REQUIRED`, `NOTE_QT_PASSAGE_FORBIDDEN`, `NOTE_VERSE_REQUIRED`)는 최신 `dev` 기준을 유지했다.

## 검증 결과

- `rg -n "NoteSaveResponse" qtai-server/src/main/java qtai-server/src/test/java qtai-server/apis/api-v1/openapi.yaml`
  - 결과: 매칭 없음
- `.\gradlew.bat test --tests com.qtai.domain.note.internal.NoteServiceTest --no-daemon` (`qtai-server/`에서 실행)
  - 결과: BUILD SUCCESSFUL
- `.\gradlew.bat test --no-daemon` (`qtai-server/`에서 실행)
  - 결과: BUILD SUCCESSFUL
- `.\gradlew.bat build --no-daemon` (`qtai-server/`에서 실행)
  - 결과: BUILD SUCCESSFUL
- `.\gradlew.bat jacocoTestReport --no-daemon`
  - 결과: 실패. 현재 `qtai-server` Gradle 루트에 `jacocoTestReport` 태스크가 등록되어 있지 않음.
- `.\gradlew.bat jacocoTestCoverageVerification --no-daemon`
  - 결과: 실패. 현재 `qtai-server` Gradle 루트에 `jacocoTestCoverageVerification` 태스크가 등록되어 있지 않음.
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`
  - 결과: 실패. 로컬에 `.spectral.yaml` 등 ruleset 파일이 없어 Spectral이 ruleset을 요구함.
- `gitleaks detect --source . --redact --exit-code 1`
  - 결과: 실패. 로컬에 `gitleaks` 명령이 설치되어 있지 않음.

## 남은 범위

- WARN/INFO 개선은 이번 범위 밖이다.
- 새 PR 본문에는 이 리포트 경로를 포함해야 한다.
