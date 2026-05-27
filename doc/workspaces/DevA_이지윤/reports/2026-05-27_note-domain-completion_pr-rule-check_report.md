# Report - 2026-05-27 note-domain-completion PR rule check

## 확인 목적

`note-domain-completion` 작업을 GitHub PR 규칙과 팀 문서 기준에 맞춰 가볍게 점검했다.

이번 점검은 완벽한 최종 QA가 아니라, PR을 올릴 때 문서 기준에서 크게 어긋나지 않도록 필요한 조율 포인트를 확인하는 데 목적이 있다.

## 확인한 규칙 파일

- `.github/workflows/qt-ai-ci.yml`
- `.github/workflows/claude-pr-review.yml`
- `.github/pull_request_template.md`
- `.github/CODEOWNERS`

위 파일은 UTF-8 기준으로 확인했다.

## PR 규칙 요약

- PR 대상 브랜치: `dev`
- 자동 머지 조건: Claude 자동 리뷰 `APPROVE` + CI 전체 success 후 squash merge
- CI 포함 항목:
  - `qtai-server` Spring build/test
  - Flutter analyze/test
  - Requirements Guard v3.1
  - gitleaks
  - OpenAPI Spectral
  - Docker Compose config 검증
- PR 본문 필수:
  - workflow 경로
  - report 경로
- CODEOWNERS 기준 owner 확인 필요

## Requirements Guard 관점 확인

현재 Note 도메인 작업 흐름은 아래 금지 항목과 직접 충돌하지 않는 것으로 판단했다.

- PostgreSQL 사용 없음
- ZooKeeper 사용 없음
- SSE / 세션형 AI 추가 없음
- RAG / Vector DB / Elasticsearch 추가 없음
- Kafka 추가 없음
- Kubernetes / Helm 추가 없음
- 개역개정 / ESV / NIV 데이터 추가 없음
- 교회 인증 F-11 구현 없음
- AI 찬양 추천 / 가사·음원 저장 없음
- 서버 Anthropic SDK 추가 없음
- `javax.*` 대신 `jakarta.*` 기준 유지
- `.env` 커밋 없음

기존 검증에서도 금지 import와 금지 번역본/본문 키워드 `rg` 검색은 매치 없음으로 확인됐다.

## Note 작업 기준 적합성

대체로 요구사항 구현 상태는 괜찮다.

- `NoteService`가 목록, 임시 노트, 상세, 생성, 수정, 삭제, 카테고리 조회 UseCase를 처리한다.
- 조회는 클래스 레벨 `@Transactional(readOnly = true)`를 기본값으로 사용하고, 생성/수정/삭제는 메서드 레벨 `@Transactional`로 분리되어 있다.
- `MEDITATION` 노트는 `qtPassageId` 필수와 활성 중복 노트 차단을 처리한다.
- `SERMON` 노트는 `verseIds` 필수 검증을 처리한다.
- 자유 노트 계열은 `qtPassageId` 전달을 차단한다.
- `note_verses`는 요청 순서 기준 중복 제거 후 저장한다.
- 삭제는 `DELETED`, `deletedAt`, `activeUniqueKey=null` 기반 소프트 삭제로 처리한다.
- Controller가 Repository를 직접 호출하지 않고 UseCase를 통해 접근한다.
- Note 도메인이 bible/qt/sharing의 `internal`/`web`을 직접 import하지 않는 경계 테스트가 있다.

## PR 본문 작성 시 필수 반영

PR 본문에는 아래 경로를 반드시 넣어야 한다.

```text
workflow: doc/workspaces/DevA_이지윤/workflows/2026-05-27_note-domain-completion.md
report: doc/workspaces/DevA_이지윤/reports/2026-05-27_note-domain-completion_report.md
```

참고로 PR 템플릿 예시는 `workspaces/{담당자}/...` 형태지만, 현재 저장소 실제 경로는 `doc/workspaces/...`이다. PR 본문에는 실제 존재하는 경로를 적는 편이 안전하다.

## CODEOWNERS 확인

Note 도메인 변경 owner:

- `qtai-server/src/main/java/com/qtai/domain/note/`
- `qtai-server/src/test/java/com/qtai/domain/note/`
- owners: `@ij447504-source @rmfdnjf98 @LeeSeung-Wook @Tae0072`

Bible 도메인 변경 owner:

- `qtai-server/src/main/java/com/qtai/domain/bible/`
- `qtai-server/src/test/java/com/qtai/domain/bible/`
- owners: `@rmfdnjf98 @LeeSeung-Wook @ij447504-source @Tae0072`

주의할 점:

- CODEOWNERS에는 `apis/` 경로가 있지만, 이번 작업의 OpenAPI 파일은 `qtai-server/apis/api-v1/openapi.yaml`이다.
- 따라서 현재 CODEOWNERS 기준으로는 OpenAPI 변경이 `apis/` 규칙에 걸리지 않고 fallback `@Tae0072`로 잡힐 가능성이 있다.
- `doc/workspaces/...`도 CODEOWNERS의 `workspaces/...` 규칙과 직접 일치하지 않아 fallback `@Tae0072`로 잡힐 가능성이 있다.
- 이 두 항목은 구현 요구사항 위반은 아니며, PR에서 owner 확인용 참고 사항으로 남기면 충분하다.

## 검증 상태

이미 확인된 성공 항목:

```bash
git diff --check
.\qtai-server\gradlew.bat -p qtai-server test --tests "*Note*"
.\qtai-server\gradlew.bat -p qtai-server test --tests "*JpaEntityDdlTest"
.\qtai-server\gradlew.bat -p qtai-server test --tests "*ArchitectureBoundaryTest"
.\qtai-server\gradlew.bat -p qtai-server build
rg -n "com\\.qtai\\.domain\\.(bible|qt|sharing)\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/note --glob "*.java"
rg -n "개역개정|ESV|NIV|성서유니온|두란노" qtai-server/src/main qtai-server/src/test qtai-server/apis/api-v1/openapi.yaml
```

실행 불가 또는 로컬 환경 이슈:

- `jacocoTestReport`: Gradle task 없음
- `jacocoTestCoverageVerification`: Gradle task 없음
- Spectral: 로컬 실행 시 `.spectral.yaml` 없음
- gitleaks: 로컬 명령어 미설치

CI에서는 `qt-ai-ci.yml` 기준으로 gitleaks와 Spectral이 별도 환경에서 실행되므로, PR 후 CI 결과를 확인하면 된다.

## 결론

현재 작업은 “요구사항이 구현됐다”는 기준에서는 충분히 괜찮다.

PR 올릴 때는 다음만 챙기면 된다.

- PR 대상은 `dev`
- PR 본문에 workflow/report 실제 경로 작성
- 관련 F-ID: `F-03`, `F-13`, `F-16`
- 테스트 방법에 통과한 Gradle 명령 기록
- CODEOWNERS 참고로 note/bible owner와 OpenAPI/doc fallback owner를 확인

팀원들과 합쳐지는 과정에서 세부 정리나 CODEOWNERS 경로 보정은 후속으로 조율해도 된다.
