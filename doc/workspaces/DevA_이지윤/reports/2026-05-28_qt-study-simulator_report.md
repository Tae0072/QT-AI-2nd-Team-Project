# 2026-05-28 qt-study-simulator report

## 기준 workflow

- `doc/workspaces/DevA_이지윤/workflows/2026-05-28_qt-study-simulator.md`

## 구현 요약

- `GET /api/v1/qt/{qtPassageId}/study-content`를 Study 도메인 컨트롤러와 UseCase로 구현했다.
- `GET /api/v1/qt/{qtPassageId}/simulator`를 Study 도메인 컨트롤러와 UseCase로 구현했다.
- `GET /api/v1/qt/{qtPassageId}/simulator-clips/{clipId}`를 04_API_명세서.md v1.7 기준 경로로 추가 구현했다.
- Study 도메인은 QT 본문 존재 여부와 verse id 목록을 `domain.qt.api.GetQtPassageContentContextUseCase`로만 조회한다.
- 승인된 `verse_explanations`, `glossary_terms`, `simulator_clips`만 사용자 응답으로 조립한다.
- 승인 시뮬레이터 클립이 없으면 검증 전 payload 없이 `MISSING`을 반환한다.

## 변경 범위

- QT 공개 context UseCase/DTO와 verse repository 추가
- Study content/simulator UseCase, DTO, Controller, Service 추가
- `glossary_terms`, `simulator_component_library_versions`, `simulator_clips` migration 추가
- Controller, Service, QT context, ArchitectureBoundary 테스트 보강
- OpenAPI `qt-study` 경로와 schema 반영

## 검증 결과

- `git diff --check`: 통과
- `.\gradlew.bat test --tests "*QtStudyContentControllerTest" --tests "*QtStudyContentServiceTest" --tests "*QtSimulatorServiceTest" --tests "*QtServiceTest" --tests "*ArchitectureBoundaryTest"`: 통과
- `.\gradlew.bat test`: 통과
- `.\gradlew.bat build`: 통과
- `.\gradlew.bat jacocoTestReport`: 실행 불가. 현재 `qtai-server` Gradle 설정에 Jacoco task가 등록되어 있지 않다.
- `.\gradlew.bat jacocoTestCoverageVerification`: 실행 불가. 현재 `qtai-server` Gradle 설정에 Jacoco task가 등록되어 있지 않다.
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`: PowerShell execution policy로 `npx.ps1` 실행 차단.
- `npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml`: 실행 불가. 저장소 루트와 `qtai-server`에 Spectral ruleset이 없다.
- `gitleaks detect --source . --redact --exit-code 1`: 실행 불가. 로컬에 `gitleaks` 명령이 설치되어 있지 않다.
- `rg -n "com\.qtai\.domain\.qt\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/study --glob "*.java"`: 위반 없음
- `rg -n "com\.qtai\.domain\.study\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/qt --glob "*.java"`: 위반 없음
- 금지 기술 검색: 신규 구현 위반 없음. 기존 `SearchBibleUseCase` 주석의 `RAG/Vector DB 금지` 문구가 검색된다.
- 금지 데이터/민감 키워드 검색: 신규 구현 위반 없음. 기존 JWT/Refresh token 테스트와 설정 키 이름에서 `token`, `password` 문자열이 검색된다.

## 후속 작업

- QT 본문 게시/노출 상태 컬럼이 생기면 `QtPassageContentContext.published`를 실제 저장값과 연결해야 한다.
- Jacoco와 Spectral ruleset을 저장소 기준 검증 명령에 맞게 구성할지 결정해야 한다.
